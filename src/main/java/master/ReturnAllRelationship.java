package master;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.logging.Log;
import org.neo4j.procedure.Context;
import org.neo4j.procedure.Procedure;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;

public class ReturnAllRelationship {
    
    @Context
    public Log log;

    @Procedure(name = "example.getAllRelationships")
    @Description("Get all relationships of a node")
    public Stream<Relationships> returnAllRelationship(@Name("node") Node node){
        List<Relationship> list = new ArrayList<>();
        node.getRelationships().iterator().forEachRemaining(rel -> list.add(rel));

        return Stream.of(new Relationships(list));
    }

    @Procedure(name = "example.getAllOutgoingNeighbours")
    @Description("Returns all neighbours on outgoing edges")
    public Stream<Nodes> returnAllOutgoingNeighbours(@Name("node") Node node){
        List<Node> outgoing = new ArrayList<>();
        node.getRelationships(Direction.OUTGOING).iterator().forEachRemaining(rel -> AddDistinct(outgoing, rel.getEndNode()));

        return Stream.of(new Nodes(outgoing));
    }

    @Procedure(name = "example.getNeighboursLHops")
    @Description("Retrieve all outgoing neighbours with l hops")
    public Stream<Nodes> getNeighboursLHops(@Name("node") Node node, @Name("l") long l){
        List<Node> finalOutgoing = new ArrayList<>();
        List<Node> outgoing = new ArrayList<>();
        List<Node> newNodes = new ArrayList<>();

        AddDistinct(outgoing, node);

        for (int i = 0; i < l; i++){
            for (int j = 0; j < outgoing.size(); j++){
                outgoing.get(j).getRelationships().iterator().forEachRemaining(rel -> AddDistinct(newNodes, rel.getEndNode()));
                AddDistinct(finalOutgoing, outgoing.get(j));
            }
            outgoing = new ArrayList<>(newNodes);
            newNodes.removeAll(newNodes);
        }
        
        return Stream.of(new Nodes(finalOutgoing));
    }

    /**
     * Adds an item to a List only if the item is not already in the List
     *
     * @param list  the list to add the distinct item to
     * @param item  the item to add to the list
     */
    private <T> void AddDistinct(List<T> list, T item){
        if(!list.contains(item))
            list.add(item);
    }

    public static class Relationships {
        // These records contain two lists of distinct relationship types going in and out of a Node.
        public List<Relationship> rels;

        public Relationships(List<Relationship> rels) {
            this.rels = rels;
        }   
    }

    public static class Nodes {
        // These records contain two lists of distinct relationship types going in and out of a Node.
        public List<Node> nodes;

        public Nodes(List<Node> nodes) {
            this.nodes = nodes;
        }   
    }
}
