"""
graphCoverageVisualization.py

Plot cumulative node-discovery coverage over time for one or more algorithm runs
stored as CSV files produced by the custom graph procedures.

Usage:
    python graphCoverageVisualization.py <folder> --hoplimit <k> [--output <path>]

Arguments:
    folder          Path to the folder containing CSV output files.
    --hoplimit, -k  (required) Only process files whose #HopLimit matches this value.
    --output, -o    (optional) Save the figure to this file path instead of displaying it.
                    Supported formats: png, pdf, svg, etc. (anything matplotlib accepts).

Example:
    python graphCoverageVisualization.py ../testoutput/testGraph-source_0-target_58 -k 4
"""

import argparse
import csv
import sys
import glob
import os
import matplotlib.pyplot as plt
import itertools
import numpy as np
from datetime import datetime

COMMENT_SIZE = 11
HEADER_SIZE = COMMENT_SIZE + 1
STEP_COUNT = 1001
PERCENT_STEP_LIST = [x*0.1 for x in range(STEP_COUNT)] + [100.0]

PLOT_FOLDER_NAME = datetime.now().strftime("%Y-%m-%dT%H-%M")

ALGORITHM_COLOR = {"IDXJOIN": "blue", "IDXDFS": "red", "CDFS": "green", "BCDFS": "orange", "JoinBCDFS": "purple", "PathEnum": "brown"}
ALGORITHM_NAMES = {"IDXJOIN": "IDX-JOIN", "IDXDFS": "IDX-DFS", "CDFS": "CDFS", "BCDFS": "BC-DFS", "JoinBCDFS": "BC-JOIN", "PathEnum": "PathEnum"}
ALGORTIHM_MARKERS = {"IDXJOIN": "o", "IDXDFS": "v", "CDFS": "s", "BCDFS": "h", "JoinBCDFS": "p", "PathEnum": "d"}

result_map: dict[str, dict[str, dict[str, dict[str, list[float]]]]] = {}
# for counting average path count and node count per query group.
avg_counts_map: dict[str, dict[str, dict[str, dict[str, float]]]] = {}

def parse_metadata(filepath):
    metadata = {}
    with open(filepath, "r", encoding="utf-8") as file:
        for line in itertools.islice(file, COMMENT_SIZE):
            content = line[1:]
            key, _, value = content.partition(":")
            metadata[key] = value.strip()
    return metadata


def parse_rows(filepath) -> list[tuple[str, int]]:
    rows = []
    with open(filepath, "r", encoding="utf-8") as file:
        for line in itertools.islice(file, HEADER_SIZE, None):
            parts = line.split(",")
            rows.append((int(parts[0]), int(parts[1])))
    return rows


def  build_plot_data(rows : list[tuple[str, int]], node_count: int, start_time: int, end_time: int) -> list[float]:

    end_ms = (end_time - start_time) / 1_000_000

    #Use step size of 0.1
    x_axis_ms = [-1.0] * STEP_COUNT

    #Start at 0%
    x_axis_ms[0] = 0.0
    x_axis_ms[STEP_COUNT-1] = end_ms

    for index, (_, discovered_at) in enumerate(rows, start=1):
        elapsed_ms = (discovered_at - start_time) / 1_000_000
        percent = round((index / node_count) * 100.0, 1)
        x_axis_ms[int(percent*10)] = elapsed_ms

    last_ms_found = end_ms
    filled_x_axis_ms = [-1.0] * STEP_COUNT
    for index, elapsed_ms in reversed(list(enumerate(x_axis_ms))):
        if elapsed_ms == -1.0:
            filled_x_axis_ms[index] = last_ms_found
        else:
            filled_x_axis_ms[index] = elapsed_ms
            last_ms_found = elapsed_ms

    filled_x_axis_ms[0] = 0.0

    return filled_x_axis_ms + [end_ms]

def main():
    parser = argparse.ArgumentParser(
        description="Plot node-discovery coverage over time from algorithm CSV output files."
    )
    parser.add_argument(
        "--folder", "-f",
        type=str,
        required=True,
    )
    args = parser.parse_args()

    with os.scandir(args.folder) as entries:
        for entry in entries:
            if entry.is_dir():

                folder_args = entry.name.split("-k_")

                dataset = folder_args[0]
                settings = folder_args[1].split("-s-t_")
                hop_limit = settings[0]
                source_target_distance = settings[1]

                dataset_map = result_map.get(dataset)
                dataset_count_map = avg_counts_map.get(dataset)

                if dataset_map is None:
                    result_map[dataset] = {}
                    dataset_map = result_map[dataset]

                if dataset_count_map is None:
                    avg_counts_map[dataset] = {}
                    dataset_count_map = avg_counts_map[dataset]

                hop_limit_map = dataset_map.get(hop_limit)
                data_hop_count_map = dataset_count_map.get(hop_limit)

                if hop_limit_map is None:
                    dataset_map[hop_limit] = {}
                    hop_limit_map = dataset_map[hop_limit]

                if data_hop_count_map is None:
                    dataset_count_map[hop_limit] = {}
                    data_hop_count_map = dataset_count_map[hop_limit]

                (hop_limit_map[source_target_distance], data_hop_count_map[source_target_distance]) = parse_folder(entry.path, dataset, int(hop_limit))
            

    for dataset, dataset_map in result_map.items():
        for hop_limit, hop_limit_map in dataset_map.items():
            for source_target_distance, plot_data in hop_limit_map.items():
                createPlot(f"{dataset} with k={hop_limit} and d(s,t)={source_target_distance}", f"{dataset}-k_{hop_limit}-l_{source_target_distance}", plot_data)

                pathenum_plot_data = {}

                cdfs_included = False

                for algorithm, algorithm_plot_data in plot_data.items():
                    createPlot(f"{dataset} with k={hop_limit} and d(s,t)={source_target_distance}", f"{algorithm}-{dataset}-k_{hop_limit}-l_{source_target_distance}", {algorithm: algorithm_plot_data})

                    if algorithm in ["IDXJOIN", "IDXDFS", "PathEnum"]:
                        pathenum_plot_data[algorithm] = algorithm_plot_data

                    if algorithm == "CDFS":
                        cdfs_included = True

                createPlot(f"{dataset} with k={hop_limit} and d(s,t)={source_target_distance}", f"PathEnumFamily-{dataset}-k_{hop_limit}-l_{source_target_distance}", pathenum_plot_data)

                if cdfs_included:
                    plot_data.pop("CDFS")
                    createPlot(f"{dataset} with k={hop_limit} and d(s,t)={source_target_distance}", f"No-CDFS-{dataset}-k_{hop_limit}-l_{source_target_distance}", pathenum_plot_data)

    write_avg_counts(avg_counts_map)




def write_avg_counts(avg_counts_map: dict):
    output_path = f"plots/{PLOT_FOLDER_NAME}/avg_counts.csv"
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)
        writer.writerow(["dataset", "hop_limit", "s_t_distance", "avg_nodecount", "avg_pathcount"])
        for dataset, hop_limit_map in avg_counts_map.items():
            for hop_limit, source_target_map in hop_limit_map.items():
                for source_target_distance, counts in source_target_map.items():
                    writer.writerow([
                        dataset,
                        hop_limit,
                        source_target_distance,
                        counts["avg_nodecount"],
                        counts["avg_pathcount"],
                    ])
    print(f"Avg counts written to {output_path}")


def parse_folder(folder: str, dataset_name: str, hop_limit: int, ):

    timed_out = {}
    count_map = {"nodecount": 0, "pathcount": 0, "querycount": 0}

    pattern = os.path.join(folder, "*.csv")
    csv_filepaths = glob.glob(pattern)

    if not csv_filepaths:
        sys.exit(f"ERROR: No CSV files found in '{folder}'")

    accepted_meta : dict[str, list[tuple[str,dict[str, str]]]] = {}
    query_coverages : dict[str, list[list[float]]] = {}
    for filepath in csv_filepaths:
        meta = parse_metadata(filepath)

        # Has never happened so commented out
        # file_hoplimit = int(meta.get("HopLimit", -1))
        # if file_hoplimit != hop_limit:
        #     print(file_hoplimit, hop_limit)
        #     continue
        # if dataset_name != meta.get("GraphName", ""):
        #     print(dataset_name, meta.get("GraphName"))
        #     continue

        #TODO: what todo with timedOut: true?
        if meta["timedOut"] == 'true':
            alg_timed_out = timed_out.get(meta["Algorithm"])

            if alg_timed_out == None:
                timed_out[meta["Algorithm"]] = 0

            timed_out[meta["Algorithm"]] += 1
            
            continue

        count_map["nodecount"] += int(meta["NodeCount"])
        count_map["pathcount"] += int(meta["PathCount"])
        count_map["querycount"] += 1

        key = f"{meta["SourceNode"]}-{meta["TargetNode"]}"

        referenceMetas : list[tuple[str, dict[str,str]]] = accepted_meta.setdefault(key, [])

        if (len(referenceMetas) != 0):
            reference_node_count = referenceMetas[0][1]["NodeCount"]
            node_count = meta["NodeCount"]

            if (reference_node_count != node_count):
                f"Error: missmatch between runs on source-target: {key} between {reference_node_count} and {filepath}"
                continue

        referenceMetas.append((filepath, meta))

        rows = parse_rows(filepath)

        x_axis_ms = build_plot_data(rows, int(meta["NodeCount"]), int(meta["StartTime"]), int(meta["EndTime"]))

        query_coverages.setdefault(meta["Algorithm"], []).append(x_axis_ms)
        

    if not accepted_meta:
        sys.exit(
            f"ERROR: No CSV files with HopLimit={hop_limit} found in '{folder}'"
        )

    

    algorithm_coverages_avg_elapsed_ms : dict[str, list[float]] = {}

    for algorithm, coverages in query_coverages.items():

        if timed_out.get(algorithm, 0) > 200:
            print(f"{algorithm} timed out over 200 for dataset {dataset_name} on k={hop_limit}")
            continue

        coverage_avg_elapsed_ms = [0.0] * len(coverages[0])
        
        query_count = len(coverages)

        for coverage in coverages:
            for index, elapsed_ms in enumerate(coverage):
                coverage_avg_elapsed_ms[index] += elapsed_ms

        for index, sum_coverage in enumerate(coverage_avg_elapsed_ms):
            coverage_avg_elapsed_ms[index] = sum_coverage/query_count

        algorithm_coverages_avg_elapsed_ms[algorithm] = coverage_avg_elapsed_ms



    return algorithm_coverages_avg_elapsed_ms, {"avg_nodecount" : count_map["nodecount"]/count_map["querycount"], "avg_pathcount": count_map["pathcount"]/ count_map["querycount"]}


def createPlot(plot_title: str, plot_name: str, plot_data: dict[str, list[float]], ):

    _, ax = plt.subplots(figsize=(10, 7))

    for algorithm, coverage_avg_elapsed_ms in plot_data.items():
        ax.step(coverage_avg_elapsed_ms, PERCENT_STEP_LIST, where="post", label=ALGORITHM_NAMES[algorithm], linewidth=2.5, color=ALGORITHM_COLOR[algorithm], marker=ALGORTIHM_MARKERS[algorithm], markersize=7, markevery=100)

    ax.set_ylim(0, 105)
    ax.set_yticks(np.linspace(0, 100, 11))
    t = ax.get_xticks(); s = t[1] - t[0]
    ax.set_xlim(-s/4, t[-1])
    ax.set_xticks(np.arange(0, t[-1], s))
    ax.tick_params(labelsize=16)
    ax.set_xlabel("Time elapsed (ms)", fontsize=20)
    ax.set_ylabel("Graph coverage (%)", fontsize=20)
    # ax.set_title(
    #     plot_title,
    #     fontsize=24,
    # )
    ax.legend(fontsize=18)
    ax.grid(True, linestyle="--", alpha=0.5, linewidth=1)
    plt.tight_layout()

    outputName = f"plots/{PLOT_FOLDER_NAME}/{plot_name}"

    os.makedirs(os.path.dirname(outputName), exist_ok=True)
    plt.savefig(outputName, dpi=150)
    print(f"Figure saved to {outputName}")

    plt.close()



if __name__ == "__main__":
    main()
