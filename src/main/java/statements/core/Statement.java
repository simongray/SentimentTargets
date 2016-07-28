package statements.core;

import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.util.CoreMap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A statement found in a natural language sentence.
 */
public class Statement implements StatementComponent {
    private Subject subject;
    private Verb verb;
    private DirectObject directObject;
    private IndirectObject indirectObject;
    private Statement embeddedStatement;
    private Set<AbstractComponent> pureComponents;
    private Set<IndexedWord> governors;
    private static DecimalFormat df = new DecimalFormat("#.##");
    private CoreMap origin;

    @Override
    public boolean equals(Object object) {
        if (object instanceof Statement) {
            Statement otherStatement = (Statement) object;
            Set<StatementComponent> otherComponents = otherStatement.getComponents();
            Statement otherEmeddedStatement = otherStatement.getEmbeddedStatement();
            boolean sameEmbeddedStatement = getEmbeddedStatement() != null? getEmbeddedStatement().equals(otherEmeddedStatement) : otherEmeddedStatement == null;
            return otherComponents.equals(getComponents()) && sameEmbeddedStatement;
        }

        return false;
    }
    
    public CoreMap getOrigin() {
        return origin;
    }

    public void setOrigin(CoreMap origin) {
        this.origin = origin;
    }

    public Statement getEmbeddedStatement() {
        return embeddedStatement;
    }

    public Statement(Set<StatementComponent> components) {
        pureComponents = new HashSet<>();  // TODO: do away with pure components entirely
        embeddedStatement = null;

        for (StatementComponent component : components) {
            add(component);
        }
    }

    private void add(StatementComponent component) {
        if (component instanceof AbstractComponent) {
            AbstractComponent abstractComponent = (AbstractComponent) component;

            if (!pureComponents.contains(abstractComponent)) {
                if (abstractComponent instanceof Subject) {
                    subject = (Subject) abstractComponent;
                } else if (abstractComponent instanceof Verb) {
                    verb = (Verb) abstractComponent;
                } else if (abstractComponent instanceof DirectObject) {
                    directObject = (DirectObject) abstractComponent;
                } else if (abstractComponent instanceof IndirectObject) {
                    indirectObject = (IndirectObject) abstractComponent;
                }
                pureComponents.add(abstractComponent);
            }
        } else if (component instanceof Statement) {
            Statement statement = (Statement) component;

            for (StatementComponent containedComponent : statement.getComponents()) {
                add(containedComponent);
            }
        } else {
            // TODO: throw exception?
        }
    }

    public Statement(Set<AbstractComponent> pureComponents, Statement embeddedStatement) {
        for (StatementComponent component : pureComponents) {
            if (component instanceof Subject) {
                subject = (Subject) component;
            } else if (component instanceof Verb) {
                verb = (Verb) component;
            } else if (component instanceof DirectObject) {
                directObject = (DirectObject) component;
            } else if (component instanceof IndirectObject) {
                indirectObject = (IndirectObject) component;
            }
        }
        this.pureComponents = pureComponents;
        this.embeddedStatement = embeddedStatement;
    }

    /**
     * The subject of the statement.
     *
     * @return subject
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * The verb of the statement.
     *
     * @return verb
     */
    public Verb getVerb() {
        return verb;
    }

    /**
     * The direct object of the statement.
     *
     * @return direct object
     */
    public DirectObject getDirectObject() {
        return directObject;
    }

    /**
     * The indirect object of the statement.
     *
     * @return indirect object
     */
    public IndirectObject getIndirectObject() {
        return indirectObject;
    }

    /**
     * The pure components making up the statement (not including any nested statement).
     *
     * @return components
     */
    public Set<AbstractComponent> getPureComponents() {
        // TODO: do away with this eventually
        return new HashSet<>(pureComponents);
    }

    /**
     * Outgoing (= governing) words.
     * Useful for establishing whether this statement is connected to another component.
     *
     * @return governors
     */
    public Set<IndexedWord> getGovernors() {
        if (governors == null) {
            governors = new HashSet<>();
            for (StatementComponent component : getComponents()) {
                governors.addAll(component.getGovernors());
            }
        }

        // remove internal governors from other components
        // otherwise, a statement can be the parent of itself
        governors.removeAll(getCompound());

        return governors;
    }

    /**
     * All components making up the statement (including any nested statement).
     *
     * @return components
     */
    public Set<StatementComponent> getComponents() {
        Set<StatementComponent> components = new HashSet<>(pureComponents);
        if (embeddedStatement != null) components.add(embeddedStatement);
        return components;
    }

    /**
     * Every word of the statement.
     *
     * @return compound
     */
    public Set<IndexedWord> getCompound() {
        Set<IndexedWord> complete = new HashSet<>();

        for (StatementComponent component : getComponents()) {
            complete.addAll(component.getCompound());
        }

        return complete;
    }

    /**
     * Every remaining word of the statement.  // TODO: revise
     *
     * @return compound
     */
    public Set<IndexedWord> getRemaining() {
        Set<IndexedWord> complete = new HashSet<>();

        for (StatementComponent component : getComponents()) {
            complete.addAll(component.getRemaining());
        }

        return complete;
    }

    public Set<IndexedWord> getWords() {
        Set<IndexedWord> complete = new HashSet<>();
        complete.addAll(getCompound());
        complete.addAll(getRemaining());

        return complete;
    }

    /**
     * The size of the statement (number of tokens).
     * Useful for sorting statements.
     *
     * @return size
     */
    public int size() {
        return getCompound().size();
    }


    /**
     * The number of components in the statement.
     *
     * @return size
     */
    public int count() {
        return getComponents().size();
    }

    /**
     * The sentence string that can be constructed from this Statement.
     *
     * @return sentence
     */
    public String getSentence() {
        return StatementUtils.join(getWords());
    }

    /**
     * A summary is a string that quickly tells you which components this Statement contains.
     *
     * @return summary
     */
    public String getSummary() {
        List<String> summary = new ArrayList<>();

        if (getSubject() != null) {
            summary.add("S");
        }
        if (getVerb() != null) {
            summary.add("V");
        }
        if (getDirectObject() != null) {
            summary.add("DO");
        }
        if (getIndirectObject() != null) {
            summary.add("IO");
        }
        if (getEmbeddedStatement() != null) {
            summary.add("E");
        }

        return String.join("+", summary);
    }


    @Override
    public String toString() {
        return "{" +
            (getLabels().isEmpty()? "" : String.join("/", getLabels()) + "/") +
            getSummary() +
            ": \"" + getSentence() + "\"" +
            ", density: " + df.format(getLexicalDensity()) +  // TODO: remove after done debugging
        "}";
    }

    /**
     * Whether this statement includes a specific component.
     *
     * @param otherComponent component to search for
     * @return true if contains component
     */
    public boolean contains(StatementComponent otherComponent) {
        if (otherComponent instanceof Statement) {
            if (getEmbeddedStatement() != null) {
                return getEmbeddedStatement().equals(otherComponent);
            }
        } else {
            return getComponents().contains(otherComponent);
        }

        return false;
    }

    /**
     * Whether this statement includes a set of components.
     *
     * @param otherComponents components to check
     * @return true if contains component
     */
    public boolean contains(Set<StatementComponent> otherComponents) {
        for (StatementComponent component : otherComponents) {
            if (!contains(component)) return false;
        }

        return true;
    }

    /**
     * Link this statement to a child statement (e.g. a dependent clause).
     *
     * @param childStatement
     */
    public Statement embed(Statement childStatement) {
        return new Statement(pureComponents, childStatement);
    }


    /**
     * The labels of a statement.
     * In most cases, statements don't have a label, but in some cases they can be useful.
     * Statement labels can be useful for knowing how to an embedded statement should be treated inside its parent.
     *
     * @return label
     */
    public Set<String> getLabels() {
        Set<String> labels = new HashSet<>();

        for (StatementComponent component : getComponents()) {
            Set<String> componentLabels = component.getLabels();

            // find component label combinations that trigger relevant statement labels
            if (componentLabels != null) {
                if (componentLabels.contains(Labels.CSUBJ_VERB)) {
                    labels.add(Labels.SUBJECT);
                } else if (componentLabels.contains(Labels.XCOMP_VERB)) {
                    labels.add(Labels.DIRECT_OBJECT);
                }
            }
        }

        // TODO: make less hacky
        // check for question marks
        if (StatementUtils.join(getWords()).endsWith("?")) labels.add(Labels.QUESTION);

        return labels;
    }

    public Set<IndexedWord> getAll() {
        Set<IndexedWord> all = new HashSet<>();
        for (StatementComponent component : getComponents()) {
            all.addAll(component.getAll());
        }

        return all;
    }

    public int getLowestIndex() {
        int lowestIndex = -1;

        for (StatementComponent component : getComponents()) {
            if (lowestIndex == -1) {
                lowestIndex = component.getLowestIndex();
            } else if (component.getLowestIndex() < lowestIndex) {
                lowestIndex = component.getLowestIndex();
            }
        }

        return lowestIndex;
    }

    public int getHighestIndex() {
        int highestIndex = 0;

        for (StatementComponent component : getComponents()) {
            if (component.getHighestIndex() > highestIndex) {
                highestIndex = component.getHighestIndex();
            }
        }

        return highestIndex;
    }

    /**
     * A basic way of comparing two statements.
     * In most cases, this will return false since it requires ALL components of the statement
     * to be matched in the other statement in their basic form.
     *
     * @param otherComponent
     * @return
     */
    @Override
    public boolean matches(StatementComponent otherComponent) {
        Set<StatementComponent> matchingComponents = new HashSet<>();

        if (otherComponent instanceof Statement) {
            Statement otherStatement = (Statement) otherComponent;
            return matchesPureComponents(otherStatement) && matchesEmbeddedStatement(otherStatement);
        }

        return false;
    }

    @Override
    public boolean isInteresting() {
        for (StatementComponent component : getComponents()) {
            if (!component.isInteresting()) return false;
        }

        return true;
    }

    @Override
    public boolean isWellFormed() {
        if (getVerb() == null) return false;

        for (StatementComponent component : getComponents()) {
            if (!component.isInteresting()) return false;
        }

        return true;
    }

    public boolean isGoodSize() {
        // Using as guide the number 12 found here: http://www.aje.com/en/arc/editing-tip-sentence-length/
        int preferredSize = 12;
        int upperLimit = preferredSize + preferredSize/4;
        int lowerLimit = preferredSize - preferredSize/4;

        return size() < upperLimit && size() > lowerLimit;
    }

    /**
     * A slightly more advanced way of comparing two statements.
     * This method of comparison tries to find statements with the same topic,
     * defined as: (S or DI matches) && (at least one other component matches)
     *
     * @param otherStatement
     * @return
     */
    public boolean matchesTopic(Statement otherStatement) {
        if (getSubject() != null && getSubject().matches(otherStatement.getSubject())
                || getDirectObject() != null && getDirectObject().matches(otherStatement.getDirectObject())) {
            return true;
        }

        return false;
    }

    /**
     * Compares the pure components of two statements.
     *
     * @param otherStatement
     * @return
     */
    private boolean matchesPureComponents(Statement otherStatement) {
        return getMatchingPureComponents(otherStatement).size() > getPureComponents().size();
    }

    /**
     * Compares the embedded statements of two statements.
     *
     * @param otherStatement
     * @return
     */
    private boolean matchesEmbeddedStatement(Statement otherStatement) {
        if (getEmbeddedStatement() == null && otherStatement.getEmbeddedStatement() == null) return true;
        if (getEmbeddedStatement() != null && otherStatement.getEmbeddedStatement() == null) return false;
        return getEmbeddedStatement().matches(otherStatement.getEmbeddedStatement());
    }

    /**
     * Returns the matching pure components of the other statement.
     *
     * @param otherStatement
     * @return
     */
    public Set<StatementComponent> getMatchingPureComponents(Statement otherStatement) {
        Set<StatementComponent> matchingComponents = new HashSet<>();

        for (AbstractComponent component : getPureComponents()) {
            for (AbstractComponent otherComponent : otherStatement.getPureComponents()) {
                if (component.matches(otherComponent)) matchingComponents.add(otherComponent);
            }
        }

        return matchingComponents;
    }

    /**
     * Returns the matching components (AbstractComponent and Statement) of the other statement.
     *
     * @param otherStatement
     * @return
     */
    public Set<StatementComponent> getMatchingComponents(Statement otherStatement) {
        Set<StatementComponent> matchingComponents = getMatchingPureComponents(otherStatement);
        if (matchesEmbeddedStatement(otherStatement) && otherStatement.getEmbeddedStatement() != null) matchingComponents.add(otherStatement.getEmbeddedStatement());
        return matchingComponents;
    }

    /**
     * Computes the lexical density of the words in the statement.
     * Based on the definitions found on: http://www.sltinfo.com/lexical-density/
     * @return the lexical density of the statement
     */
    public double getLexicalDensity() {
        double lexicalWords = 0;
        Set<IndexedWord> words = getWords();

        for (IndexedWord word : words) {
            if (Tags.LEXICAL_WORDS.contains(word.tag())) lexicalWords++;
        }

        return lexicalWords / (double) words.size();
    }

    /**
     * A subjective score used to rank statements.
     * The baseline used is lexical density, which is then adjusted based on certain heuristics.
     *
     * @return quality score
     */
    public double getQuality() {
        double quality = getLexicalDensity();  // baseline

        quality = adjustForWellformedness(quality);
        quality = adjustForSize(quality);

        return quality;
    }

    /**
     * Adjusts the baseline quality score based on the wellformedness of the statement.
     *
     * @param quality the score to adjust
     * @return the adjusted score
     */
    private double adjustForWellformedness(double quality) {
        if (isWellFormed()) {
            quality += getUpwardsAdjustment(quality);
        } else {
            quality -= getDownwardsAdjustment(quality);
        }

        return quality;
    }

    /**
     * Adjusts the baseline quality score based on length of the statement.
     *
     * @param quality the score to adjust
     * @return the adjusted score
     */
    private double adjustForSize(double quality) {
        if (isGoodSize()) {
            quality += getUpwardsAdjustment(quality);
        } else {
            quality -= getDownwardsAdjustment(quality);
        }

        return quality;
    }

    private double getAdjustmentMultiplier() {
        return 0.20;   // TODO: this is an arbitrary value!
    }

    public double getUpwardsAdjustment(double quality) {
        double remainder = 1.0 - quality;
        double adjustment = remainder * getAdjustmentMultiplier();
        return adjustment;
    }

    public double getDownwardsAdjustment(double quality) {
        return quality * getAdjustmentMultiplier();
    }
}
