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
import sys
import glob
import os
import matplotlib.pyplot as plt
import itertools

def parse_metadata(filepath):
    metadata = {}
    with open(filepath, "r", encoding="utf-8") as file:
        for line in itertools.islice(file, 8):
            content = line[1:]
            key, _, value = content.partition(":")
            metadata[key] = value.strip()
    return metadata


def parse_rows(filepath):
    rows = []
    with open(filepath, "r", encoding="utf-8") as file:
        for line in itertools.islice(file, 9, None):
            parts = line.split(",")
            rows.append((int(parts[0]), int(parts[1])))
    return rows


def required_meta_int(meta, key, filepath):
    if key not in meta:
        sys.exit(f"ERROR: '{key}' not found in metadata of {filepath}")
    try:
        return int(meta[key])
    except ValueError:
        sys.exit(f"ERROR: '{key}' is not an integer in {filepath}  (got '{meta[key]}')")


def build_plot_data(parsed, node_count, start_time, end_time):
    rows = parsed["rows"]

    x_axis_ms = [0.0]
    y_axis_percent = [0.0]

    for index, (_, discovered_at) in enumerate(rows, start=1):
        elapsed_ms = (discovered_at - start_time) / 1_000_000
        percent = (index / node_count) * 100.0
        x_axis_ms.append(elapsed_ms)
        y_axis_percent.append(percent)

    end_ms = (end_time - start_time) / 1_000_000
    if x_axis_ms[-1] < end_ms:
        x_axis_ms.append(end_ms)
        y_axis_percent.append(100.0)

    return x_axis_ms, y_axis_percent

def main():
    parser = argparse.ArgumentParser(
        description="Plot node-discovery coverage over time from algorithm CSV output files."
    )
    parser.add_argument(
        "--folder", "-f",
        type=str,
        required=True,
    )
    parser.add_argument(
        "--hoplimit", "-k",
        type=int,
        required=True,
    )
    parser.add_argument(
        "--show", "-s",
        action=argparse.BooleanOptionalAction,
    )
    args = parser.parse_args()

    pattern = os.path.join(args.folder, "*.csv")
    csv_filepaths = glob.glob(pattern)

    if not csv_filepaths:
        sys.exit(f"ERROR: No CSV files found in '{args.folder}'")

    accepted_meta = []
    for filepath in csv_filepaths:
        meta = parse_metadata(filepath)

        file_hoplimit = int(meta.get("HopLimit"))
        if file_hoplimit != args.hoplimit:
            continue

        accepted_meta.append((filepath, meta))

    if not accepted_meta:
        sys.exit(
            f"ERROR: No CSV files with HopLimit={args.hoplimit} found in '{args.folder}'"
        )

    ref_path, reference = accepted_meta[0]

    for filepath, meta in accepted_meta[1:]:
            if meta.get("NodeCount") != reference.get("NodeCount"):
                sys.exit(
                    f"ERROR: Inconsistent 'NodeCount' between files.\n"
                    f"  {ref_path}: {reference.get("NodeCount")}\n"
                    f"  {filepath}: {meta.get("NodeCount")}"
                )

    graph_name = reference.get("GraphName")
    source_node = reference.get("SourceNode")
    target_node = reference.get("TargetNode")
    node_count = int(reference.get("NodeCount"))
    hop_limit = int(reference.get("HopLimit"))

    _, ax = plt.subplots(figsize=(10, 6))

    for filepath, meta in accepted_meta:
        start_time = int(meta.get("StartTime"))
        end_time = int(meta.get("EndTime"))
        algorithm = meta.get("Algorithm")

        rows = parse_rows(filepath)
        x_axis_ms, y_axis_percent = build_plot_data({"rows": rows}, node_count, start_time, end_time)
        ax.step(x_axis_ms, y_axis_percent, where="post", label=algorithm, linewidth=2)

    ax.set_xlabel("Time elapsed (ms)", fontsize=12)
    ax.set_ylabel("Graph coverage (%)", fontsize=12)
    ax.set_title(
        f"{graph_name}  —  k={args.hoplimit}  |  {source_node} to {target_node}",
        fontsize=13,
    )
    ax.set_ylim(0, 105)
    ax.legend(fontsize=11)
    ax.grid(True, linestyle="--", alpha=0.5)
    plt.tight_layout()

    outputName = f"plots/{graph_name}-k{hop_limit}-source{source_node}-target{target_node}"

    os.makedirs(os.path.dirname(outputName), exist_ok=True)
    plt.savefig(outputName, dpi=150)
    print(f"Figure saved to {outputName}")

    if(args.show):
        plt.show()


if __name__ == "__main__":
    main()
