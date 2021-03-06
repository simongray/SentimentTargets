package statements.core;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.semgraph.SemanticGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Allows for linking of Subject, Verb, DirectObject, and IndirectObject.
 */
public interface StatementComponent {
    Set<IndexedWord> getCompound();
    Set<IndexedWord> getRemaining();  // TODO: rename and find a stronger purpose than just getting clauses into the statement text
    Set<IndexedWord> getAll();
    Set<IndexedWord> getGovernors();
    Set<IndexedWord> getEmbeddingGovernors();  // TODO: rename, sounds like crap
    Set<String> getLabels();
    boolean contains(StatementComponent otherComponent);  // TODO: do or do not?

    /**
     * Is this component the parent of another component?
     * It will evaluate to false in case part of the other component
     *
     * @param otherComponent the other component
     * @return true if parent of
     */
    default boolean parentOf(StatementComponent otherComponent) {
        return StatementUtils.intersects(getCompound(), otherComponent.getGovernors()) && !StatementUtils.intersects(getCompound(), otherComponent.getCompound());
    }

    /**
     * Is this component the parent of another component?
     * It will evaluate to false in case part of the other component
     *
     * @param otherComponent the other component
     * @return true if parent of
     */
    default boolean embeddingParentOf(StatementComponent otherComponent) {
        return StatementUtils.intersects(getCompound(), otherComponent.getEmbeddingGovernors()) && !StatementUtils.intersects(getCompound(), otherComponent.getCompound());
    }
}
