package com.ibm.ets.ita.ce.store.conversation.model;

/*******************************************************************************
 * (C) Copyright IBM Corporation  2011, 2016
 * All Rights Reserved
 *******************************************************************************/

import static com.ibm.ets.ita.ce.store.utilities.GeneralUtilities.stripDelimitingQuotesFrom;
import static com.ibm.ets.ita.ce.store.utilities.ReportingUtilities.reportMicroDebug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeMap;

import com.ibm.ets.ita.ce.store.ActionContext;
import com.ibm.ets.ita.ce.store.client.web.ServletStateManager;
import com.ibm.ets.ita.ce.store.hudson.handler.GenericHandler;
import com.ibm.ets.ita.ce.store.hudson.helper.ConvConfig;
import com.ibm.ets.ita.ce.store.hudson.helper.HudsonManager;
import com.ibm.ets.ita.ce.store.hudson.helper.WordCheckerCache;
import com.ibm.ets.ita.ce.store.model.CeConcept;
import com.ibm.ets.ita.ce.store.model.CeInstance;
import com.ibm.ets.ita.ce.store.model.CeProperty;

public class ProcessedWord extends GeneralItem {
	public static final String copyrightNotice = "(C) Copyright IBM Corporation  2011, 2016";

	private static final String CON_ENTCON = "entity concept";
	private static final String CON_PROPCON = "property concept";
	private static final String PROP_PLURAL = "plural form";
	private static final String PROP_PAST = "past tense";
	private static final String PROP_EXPBY = "is expressed by";

	private ConvWord convWord = null;
	private String lcText = null;
	private int wordPos = -1;
	private boolean isNumberWord = false;
	private String matchedText = null;

	private ArrayList<MatchedItem> matchedItems = new ArrayList<MatchedItem>();
	private ArrayList<MatchedItem> otherMatchedItems = new ArrayList<MatchedItem>();

//	private static final String CON_PROWORD = "processed word";
//	private static final String CON_UNMWORD = "unmatched word";
//	private static final String DET_A = "a";
//	private static final String DET_AN = "an";
//	private static final String DET_THE = "the";
//	private static final String Q_WHAT = "what";
//	private static final String Q_WHO = "who";
//	private static final String Q_WHERE = "where";
//	private static final String Q_WHICH = "which";
//	private static final String Q_WHY = "why";
//	private static final String Q_COUNT = "count";
//	private static final String Q_LIST = "list";
//	private static final String Q_SUMM1 = "summarise";
//	private static final String Q_SUMM2 = "summarize";
//	private static final String Q_ALL = "all";

//	private ArrayList<ExtractedItem> extractedItems = null;
	// Matched (directly, by the name)
//	private CeConcept matchingConcept = null;
//	private TreeMap<String, CeProperty> matchingRelations = null;
//	private ArrayList<CeInstance> matchingInstances = null;

	// Referred (indirectly
//	private TreeMap<String, CeConcept> referredExactConcepts = null;
//	private TreeMap<String, CeProperty> referredExactRelations = null;
//	private TreeMap<String, CeInstance> referredExactInstances = null;
//	private TreeMap<String, CeConcept> referredExactConceptsPlural = null;
//	private TreeMap<String, CeConcept> referredExactConceptsPastTense = null;
//	private TreeMap<String, CeInstance> referredExactInstancesPlural = null;

	// Types of words
//	private boolean isStandardWord = false;
//	private boolean isNegationWord = false;
//	private boolean isValueWord = false;
//	private boolean isQuestionWord = false;

//	private boolean partialStartWord = false;
//	private boolean partialConceptReference = false;
//	private boolean partialRelationReference = false;
//	private boolean partialInstanceReference = false;

//	private CeInstance chosenInstance = null;
//	private String tempLabel = null;

	private ProcessedWord(ConvWord pConvWord, int pWordPos) {
		this.id = pConvWord.getId();
		this.convWord = pConvWord;
		this.wordPos = pWordPos;

		this.lcText = declutter(pConvWord.getWordText().toLowerCase());

		try {
			// Attempt to create a double
			Double.parseDouble(getDeclutteredText());
			this.isNumberWord = true;
		} catch (NumberFormatException e) {
			// There was a number format exception so this is not a number word
			this.isNumberWord = false;
		}
	}

	public int getWordPos() {
		return this.wordPos;
	}

	public static ProcessedWord createFrom(ConvWord pConvWord, int pWordPos) {
		ProcessedWord newGw = new ProcessedWord(pConvWord, pWordPos);

		pConvWord.setProcessedWord(newGw);

		return newGw;
	}

	public ConvWord getConvWord() {
		return this.convWord;
	}

	public String getWordText() {
		return this.convWord.getWordText();
	}

	public CeConcept getMatchingConcept() {
		CeConcept result = null;

		for (MatchedItem mi : this.matchedItems) {
			if (mi.isMatchedConcept()) {
				result = mi.getConcept();
				break;
			}
		}

		return result;
	}

	public void setMatchingConcept(CeConcept pCon) {
		saveMatchedItem(MatchedItem.createForMatchedConcept(this, pCon));

//		this.matchingConcept = pCon;
	}

	private void saveMatchedItem(MatchedItem pMi) {
		this.matchedItems.add(pMi);
	}

	public ArrayList<CeInstance> getMatchingInstances() {
		ArrayList<CeInstance> result = new ArrayList<CeInstance>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isMatchedInstance()) {
				result.add(mi.getInstance());
			}
		}

		return result;
	}

	public void setMatchingInstances(ArrayList<CeInstance> pInsts) {
		for (CeInstance thisInst : pInsts) {
			saveMatchedItem(MatchedItem.createForMatchedInstance(this, thisInst));
		}

//		this.matchingInstances = pInsts;
	}

	public CeInstance getFirstMatchingInstance() {
		CeInstance result = null;

		if (!getMatchingInstances().isEmpty()) {
			result = getMatchingInstances().get(0);
		}

		return result;
	}

	public boolean matchesToConcept() {
		return getMatchingConcept() != null;
	}

	public boolean matchesToRelations() {
		return (getMatchingRelations() != null) && !getMatchingRelations().isEmpty();
	}

	public boolean matchesToInstance() {
		return !getMatchingInstances().isEmpty();
	}

	public boolean refersToConceptsExactly() {
		return !getReferredExactConcepts().isEmpty();
	}

	public boolean refersToInstanceOfConceptNamed(ActionContext pAc, String pConName) {
		boolean result = false;

		for (CeInstance thisInst : getMatchingInstances()) {
			if (thisInst.isConceptNamed(pAc, pConName)) {
				result = true;
				break;
			}
		}

		return result;
	}

	public boolean refersToPluralConceptsExactly() {
		return !getReferredExactConceptsPlural().isEmpty();
	}

	public boolean refersToPastTenseConceptsExactly() {
		return !getReferredExactConceptsPastTense().isEmpty();
	}

	public boolean refersToPluralInstancesExactly() {
		return !getReferredExactInstancesPlural().isEmpty();
	}

	public boolean refersToRelationsExactly() {
		return !getReferredExactRelations().isEmpty();
	}

	public boolean refersToInstancesExactly() {
		return !getReferredExactInstances().isEmpty();
	}

	public String getLcWordText() {
		return this.lcText;
	}

	public boolean isNumberWord() {
		return this.isNumberWord;
	}

	public boolean isUnmatchedWord() {
//		return (getMatchingConcept() == null) && !matchesToRelations() && (getMatchingInstances().isEmpty())
//				&& !refersToConceptsExactly() && !refersToPluralConceptsExactly() && !refersToRelationsExactly()
//				&& !refersToInstancesExactly() && !refersToPluralInstancesExactly() && (!this.isStandardWord)
//				&& (!this.isNegationWord) && (!this.isValueWord) && (!this.isNumberWord)
//				&& (!this.partialConceptReference) && (!this.partialRelationReference)
//				&& (!this.partialInstanceReference) && (!isDeterminer()) && (!isQuestionWord());
		return (getMatchingConcept() == null) && !matchesToRelations() && (getMatchingInstances().isEmpty())
				&& !refersToConceptsExactly() && !refersToPluralConceptsExactly() && !refersToRelationsExactly()
				&& !refersToInstancesExactly() && !refersToPluralInstancesExactly()
				&& !this.isNumberWord;
//				&& ((!isDeterminer()));
	}

	public String getDeclutteredText() {
		return this.lcText;
	}

	private static String declutter(String pRawText) {
		String result = stripDelimitingQuotesFrom(pRawText);

		return result;
	}

	public void classify(ActionContext pAc, ConvConfig pCc) {
		WordCheckerCache wcc = ServletStateManager.getHudsonManager(pAc).getWordCheckerCache();

		wcc.checkForMatchingConcept(pAc, this);
		wcc.checkForMatchingRelation(pAc, this);
		wcc.checkForMatchingInstances(pAc, this);

		checkForReferringConcepts(pAc);
		checkForReferringRelations(pAc);
		checkForReferringInstances(pAc, wcc);

//		checkForStandardWords(pCc, wcc, pAc);

		String decText = getDeclutteredText();

		checkForPartialMatchingConcepts(pAc, decText);
		checkForPartialMatchingRelations(pAc, decText);
		checkForPartialMatchingInstances(pAc, decText);
	}

	private void checkForReferringConcepts(ActionContext pAc) {
		for (CeInstance thisInst : pAc.getModelBuilder().getAllInstancesForConceptNamed(pAc, CON_ENTCON)) {
			checkForConceptByExpressedBy(pAc, thisInst);
			checkForConceptByPluralForm(pAc, thisInst);
			checkForConceptByPastTense(pAc, thisInst);
		}
	}

	private void checkForConceptByExpressedBy(ActionContext pAc, CeInstance pEntConInst) {
		String conName = pEntConInst.getInstanceName();
		String decText = getDeclutteredText().toLowerCase();
		ArrayList<String> expList = pEntConInst.getValueListFromPropertyNamed(PROP_EXPBY);
		ArrayList<String> lcExpList = new ArrayList<String>();

		for (String thisExp : expList) {
			lcExpList.add(thisExp.toLowerCase());
		}

		for (String expVal : lcExpList) {
			boolean match = expVal.equals(decText);
			boolean exact = false;

			if (match) {
				exact = true;
			} else {
				exact = false;
				match = expVal.startsWith(decText);
			}

			CeConcept tgtCon = pAc.getModelBuilder().getConceptNamed(pAc, conName);

			if (tgtCon != null) {
				if (match) {
					if (exact) {
						if (getMatchingConcept() != tgtCon) {
							addReferredExactConcept(pAc, decText, tgtCon);
						}
					} else {
						ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();
						if (testForFullConceptReferenceWithLaterWords(pAc, decText, lcExpList, 1, otherWords)) {
							reportMicroDebug("Found multiple word concept reference at: " + decText, pAc);
							this.addReferredExactConceptFromMultipleWords(pAc, conName, tgtCon, otherWords);
						}
					}
				}
			}
		}
	}

	private void checkForInstanceByExpressedBy(ActionContext pAc, CeInstance pPossInst) {
		String decText = getDeclutteredText().toLowerCase();

		specificCheckForInstanceByExpressedBy(pAc, pPossInst, decText, null);

		String trimmedText = depluralise(decText);

		if (trimmedText != null) {
			specificCheckForInstanceByExpressedBy(pAc, pPossInst, trimmedText, decText);
		}
	}

	public String depluralise(String pRawText) {
		String result = null;

		//Quick and dirty de-pluralisation
		if (pRawText.endsWith("'s")) {
			result = pRawText.substring(0, pRawText.length() - 2);
		} else if (pRawText.endsWith("s")) {
			result = pRawText.substring(0, pRawText.length() - 1);
		} else if (pRawText.endsWith("'")) {
			result = pRawText.substring(0, pRawText.length() - 1);
		}

		return result;
	}

	private void specificCheckForInstanceByExpressedBy(ActionContext pAc, CeInstance pPossInst, String pDecText, String pLongText) {
		ArrayList<String> expList = pPossInst.getValueListFromPropertyNamed(PROP_EXPBY);
		ArrayList<String> lcExpList = new ArrayList<String>();

		this.matchedText = null;

		for (String thisExp : expList) {
			lcExpList.add(thisExp.toLowerCase());
		}

		for (String expVal : lcExpList) {
			boolean match = expVal.equals(pDecText);
			boolean exact = false;

			if (match) {
				exact = true;
			} else {
				exact = false;
				match = expVal.startsWith(pDecText);
			}

			if (match) {
				if (exact) {
					// This is an exact match so just save the referenced
					// instance
					if (pLongText != null) {
						this.addReferredExactInstance(pLongText, pPossInst);
					} else {
						this.addReferredExactInstance(expVal, pPossInst);
					}
				} else {
					ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();

					if (testForFullInstanceReferenceWithLaterWords(pAc, pDecText, lcExpList, 1, otherWords)) {
						reportMicroDebug("Found multiple word instance reference at: " + pDecText, pAc);

						if (pLongText != null) {
							this.addReferredExactInstanceFromMultipleWords(pLongText, pPossInst, otherWords);
						} else {
							if (this.matchedText != null) {
								this.addReferredExactInstanceFromMultipleWords(this.matchedText, pPossInst, otherWords);
							} else {
								this.addReferredExactInstanceFromMultipleWords(expVal, pPossInst, otherWords);
							}
						}
					}
				}
			}
		}
	}

	private void checkForConceptByPluralForm(ActionContext pAc, CeInstance pEntConInst) {
		String conName = pEntConInst.getInstanceName();
		String decText = getDeclutteredText();

		ArrayList<String> expList = pEntConInst.getValueListFromPropertyNamed(PROP_PLURAL);
		ArrayList<String> lcExpList = new ArrayList<String>();

		for (String thisExp : expList) {
			lcExpList.add(thisExp.toLowerCase());
		}

		for (String expVal : lcExpList) {
			boolean match = expVal.equals(decText);
			boolean exact = false;

			if (match) {
				exact = true;
			} else {
				exact = false;
				match = expVal.startsWith(decText);
			}

			CeConcept tgtCon = pAc.getModelBuilder().getConceptNamed(pAc, conName);

			if (match) {
				if (exact) {
					if (getMatchingConcept() != tgtCon) {
						addReferredExactConceptPlural(pAc, decText, tgtCon);
					}
				} else {
					ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();

					if (testForFullConceptReferenceWithLaterWords(pAc, decText, lcExpList, 1, otherWords)) {
						reportMicroDebug("Found multiple word concept reference at: " + decText, pAc);
						this.addReferredExactConceptPluralFromMultipleWords(pAc, conName, tgtCon, otherWords);
					}
				}
			}
		}
	}

	private void checkForConceptByPastTense(ActionContext pAc, CeInstance pEntConInst) {
		String conName = pEntConInst.getInstanceName();
		String decText = getDeclutteredText();

		ArrayList<String> expList = pEntConInst.getValueListFromPropertyNamed(PROP_PAST);
		ArrayList<String> lcExpList = new ArrayList<String>();

		for (String thisExp : expList) {
			lcExpList.add(thisExp.toLowerCase());
		}

		for (String expVal : lcExpList) {
			boolean match = expVal.equals(decText);
			boolean exact = false;

			if (match) {
				exact = true;
			} else {
				exact = false;
				match = expVal.startsWith(decText);
			}

			CeConcept tgtCon = pAc.getModelBuilder().getConceptNamed(pAc, conName);

			if (match) {
				if (exact) {
					if (getMatchingConcept() != tgtCon) {
						addReferredExactConceptPastTense(pAc, conName, tgtCon);
					}
				} else {
					ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();

					if (testForFullConceptReferenceWithLaterWords(pAc, decText, lcExpList, 1, otherWords)) {
						reportMicroDebug("Found multiple word concept reference at: " + decText, pAc);
						this.addReferredExactConceptPastTenseFromMultipleWords(pAc, conName, tgtCon, otherWords);
					}
				}
			}
		}
	}

	public ArrayList<MatchedItem> getMatchedItems() {
		return this.matchedItems;
	}

	public ArrayList<MatchedItem> getOtherMatchedItems() {
		return this.otherMatchedItems;
	}

	public void addOtherMatchedItem(MatchedItem pItem) {
		if (!this.otherMatchedItems.contains(pItem)) {
			this.otherMatchedItems.add(pItem);
		}
	}

	public boolean hasOtherMatchedItems() {
		return !this.otherMatchedItems.isEmpty();
	}

	public TreeMap<String, CeProperty> getMatchingRelations() {
		TreeMap<String, CeProperty> result = new TreeMap<String, CeProperty>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isMatchedProperty()) {
				result.put(mi.getPhraseText(), mi.getProperty());
			}
		}
		
		return result;
	}

	public void setMatchingRelations(TreeMap<String, CeProperty> pProps) {
//		this.matchingRelations = pProps;
		
		for (String thisKey : pProps.keySet()) {
			CeProperty thisProp = pProps.get(thisKey);
			saveMatchedItem(MatchedItem.createForMatchedProperty(this, thisProp));
		}
	}

	public Collection<CeProperty> listMatchingRelations() {
		Collection<CeProperty> result = null;

		if (getMatchingRelations() != null) {
			result = getMatchingRelations().values();
		} else {
			result = new ArrayList<CeProperty>();
		}

		return result;
	}

	private boolean alreadyRefersToExactRelation(CeProperty pProp) {
		boolean result = false;

		if (refersToRelationsExactly()) {
			result = getReferredExactRelations().containsValue(pProp);
		} else {
			result = false;
		}

		return result;
	}

	private void checkForReferringRelations(ActionContext pAc) {
		for (CeInstance thisInst : pAc.getModelBuilder().getAllInstancesForConceptNamed(pAc, CON_PROPCON)) {
			String propFullName = thisInst.getInstanceName();
			String decText = getDeclutteredText();

			ArrayList<String> expList = thisInst.getValueListFromPropertyNamed(PROP_EXPBY);

			if (!expList.isEmpty()) {
				ArrayList<String> lcExpList = new ArrayList<String>();

				for (String thisExp : expList) {
					lcExpList.add(thisExp.toLowerCase());
				}

				for (String expVal : lcExpList) {
					boolean match = expVal.equals(decText);
					boolean exact = false;

					if (match) {
						exact = true;
					} else {
						exact = false;
						match = expVal.startsWith(decText);
					}

					if (match) {
						if (exact) {
							CeProperty tgtProp = pAc.getModelBuilder().getPropertyFullyNamed(propFullName);
							if (!alreadyMatchesRelation(tgtProp)) {
								addReferredExactRelation(pAc, propFullName, tgtProp);
							}
						} else {
							CeProperty tgtProp = pAc.getModelBuilder().getPropertyFullyNamed(propFullName);
							if (tgtProp != null) {
								if (!alreadyMatchesRelation(tgtProp)) {
									if (!alreadyRefersToExactRelation(tgtProp)) {
										ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();

										if (testForFullRelationReferenceWithLaterWords(pAc, decText, lcExpList, 1, otherWords)) {
											reportMicroDebug("Found multiple word relation reference at: " + decText,
													pAc);
											addReferredExactRelationFromMultipleWords(pAc, expVal, tgtProp, otherWords);
										}
									}
								}
							} else {
								// TODO: Do this properly
								if (propFullName.equals("special:inheritance")) {
									reportMicroDebug("I found the special inheritance relationship", pAc);
								}
							}
						}
					}
				}
			}
		}
	}

	private boolean testForFullRelationReferenceWithLaterWords(ActionContext pAc, String pLcWordText,
			ArrayList<String> pExpList, int pDepth, ArrayList<ProcessedWord> pOtherWords) {
		boolean result = false;
		int depth = pDepth;
		ProcessedWord nextWord = getNextProcessedWord();

		if (nextWord != null) {
			String concatText = pLcWordText + " " + nextWord.getDeclutteredText();
			pOtherWords.add(nextWord);

			if (pExpList.contains(concatText)) {
				reportMicroDebug("Matched '" + concatText + "' at depth " + pDepth, pAc);

				nextWord.markWordsAsRelationReferenceMatchedToDepth(pDepth);
				result = true;
			} else {
				result = nextWord.testForFullRelationReferenceWithLaterWords(pAc, concatText, pExpList, ++depth, pOtherWords);
			}
		} else {
			result = false;
		}

		return result;
	}

	private boolean testForFullConceptReferenceWithLaterWords(ActionContext pAc, String pLcWordText,
			ArrayList<String> pExpList, int pDepth, ArrayList<ProcessedWord> pOtherWords) {
		boolean result = false;
		int depth = pDepth;
		ProcessedWord nextWord = getNextProcessedWord();

		if (nextWord != null) {
			String concatText = pLcWordText + " " + nextWord.getDeclutteredText();

			if (pExpList.contains(concatText)) {
				reportMicroDebug("Matched '" + concatText + "' at depth " + pDepth, pAc);

				nextWord.markWordsAsConceptReferenceMatchedToDepth(pDepth);
				pOtherWords.add(nextWord);
				result = true;
			} else {
				result = nextWord.testForFullConceptReferenceWithLaterWords(pAc, concatText, pExpList, ++depth, pOtherWords);
			}
		} else {
			result = false;
		}

		return result;
	}

	private boolean testForFullInstanceReferenceWithLaterWords(ActionContext pAc, String pLcWordText, ArrayList<String> pExpList, int pDepth, ArrayList<ProcessedWord> pOtherWords) {
		boolean result = false;
		ProcessedWord nextWord = getNextProcessedWord();

		if (nextWord != null) {
			String concatText = pLcWordText + " " + nextWord.getDeclutteredText();

			result = doInstRefTest(pAc, pExpList, pDepth, nextWord, concatText, pOtherWords);

			if (!result) {
				String depText = depluralise(concatText);

				if (depText != null) {
					result = doInstRefTest(pAc, pExpList, pDepth, nextWord, depText, pOtherWords);

					this.matchedText = concatText;
				}
			}
		} else {
			result = false;
		}

		return result;
	}

	private boolean doInstRefTest(ActionContext pAc, ArrayList<String> pExpList, int pDepth, ProcessedWord pNextWord, String pConcatText, ArrayList<ProcessedWord> pOtherWords) {
		boolean result = false;
		int depth = pDepth;

		if (pExpList.contains(pConcatText)) {
			reportMicroDebug("Matched '" + pConcatText + "' at depth " + pDepth, pAc);

			pOtherWords.add(pNextWord);
			pNextWord.markWordsAsInstanceReferenceMatchedToDepth(pDepth);
			result = true;
		} else {
			result = pNextWord.testForFullInstanceReferenceWithLaterWords(pAc, pConcatText, pExpList, ++depth, pOtherWords);
		}

		return result;
	}

	public void markWordsAsRelationReferenceMatchedToDepth(int pDepth) {
		if (pDepth > 0) {
			int depth = pDepth;
//			this.partialRelationReference = true;
			ProcessedWord prevWord = getPreviousProcessedWord();

			if (prevWord != null) {
				prevWord.markWordsAsRelationReferenceMatchedToDepth(--depth);
			}
		}
	}

	public void markWordsAsConceptReferenceMatchedToDepth(int pDepth) {
		if (pDepth > 0) {
			int depth = pDepth;
//			this.partialConceptReference = true;
			ProcessedWord prevWord = getPreviousProcessedWord();

			if (prevWord != null) {
				prevWord.markWordsAsConceptReferenceMatchedToDepth(--depth);
			}
		}
	}

	public void markWordsAsInstanceReferenceMatchedToDepth(int pDepth) {
		if (pDepth > 0) {
			int depth = pDepth;
//			this.partialInstanceReference = true;
			ProcessedWord prevWord = getPreviousProcessedWord();

			if (prevWord != null) {
				prevWord.markWordsAsInstanceReferenceMatchedToDepth(--depth);
			}
		}
	}

	private void checkForReferringInstances(ActionContext pAc, WordCheckerCache pWcc) {
		checkForPluralMatchingInstance(pAc, getDeclutteredText(), pWcc);

		for (CeInstance thisInst : pWcc.getLingThingInstances(pAc)) {
			if (!thisInst.isMetaModelInstance()) {
				checkForInstanceByExpressedBy(pAc, thisInst);
			}
		}
	}

	private TreeMap<String, CeConcept> getReferredExactConcepts() {
		TreeMap<String, CeConcept> result = new TreeMap<String, CeConcept>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isReferredConceptExact()) {
				result.put(mi.getPhraseText(), mi.getConcept());
			}
		}
		
		return result;
	}
	
	private TreeMap<String, CeConcept> getReferredExactConceptsPlural() {
		TreeMap<String, CeConcept> result = new TreeMap<String, CeConcept>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isReferredConceptPlural()) {
				result.put(mi.getPhraseText(), mi.getConcept());
			}
		}
		
		return result;
	}

	private TreeMap<String, CeInstance> getReferredExactInstancesPlural() {
		TreeMap<String, CeInstance> result = new TreeMap<String, CeInstance>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isReferredInstancePlural()) {
				result.put(mi.getPhraseText(), mi.getInstance());
			}
		}
		
		return result;
	}

	private TreeMap<String, CeConcept> getReferredExactConceptsPastTense() {
		TreeMap<String, CeConcept> result = new TreeMap<String, CeConcept>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isReferredConceptPastTense()) {
				result.put(mi.getPhraseText(), mi.getConcept());
			}
		}
		
		return result;
	}

	private TreeMap<String, CeProperty> getReferredExactRelations() {
		TreeMap<String, CeProperty> result = new TreeMap<String, CeProperty>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isReferredPropertyExact()) {
				result.put(mi.getPhraseText(), mi.getProperty());
			}
		}
		
		return result;
	}

	private TreeMap<String, CeInstance> getReferredExactInstances() {
		TreeMap<String, CeInstance> result = new TreeMap<String, CeInstance>();
		
		for (MatchedItem mi : this.matchedItems) {
			if (mi.isReferredInstanceExact()) {
				result.put(mi.getPhraseText(), mi.getInstance());
			}
		}
		
		return result;
	}

	private void addReferredExactConceptFromMultipleWords(ActionContext pAc, String pConName, CeConcept pTgtCon, ArrayList<ProcessedWord> pOtherWords) {
		MatchedItem thisMi = MatchedItem.createForReferredConceptExact(this, pTgtCon, pConName, pOtherWords);

		saveMatchedItem(thisMi);
//		this.partialStartWord = true;
	}

	private void addReferredExactConcept(ActionContext pAc, String pConName, CeConcept pTgtCon) {
		MatchedItem thisMi = MatchedItem.createForReferredConceptExact(this, pTgtCon, pConName);

		saveMatchedItem(thisMi);

//		if (pTgtCon != null) {
//			if (getReferredExactConcepts() == null) {
//				this.referredExactConcepts = new TreeMap<String, CeConcept>();
//			}
//
//			this.referredExactConcepts.put(pConName, pTgtCon);
//		} else {
//			reportError("Unable to add exact concept '" + pConName + "' as it could not be located", pAc);
//		}
	}

	public Collection<CeConcept> listReferredExactConcepts() {
		Collection<CeConcept> result = null;

		if (!getReferredExactConcepts().isEmpty()) {
			result = getReferredExactConcepts().values();
		} else {
			result = new ArrayList<CeConcept>();
		}

		return result;
	}

	private void addReferredExactConceptPluralFromMultipleWords(ActionContext pAc, String pConName, CeConcept pTgtCon, ArrayList<ProcessedWord> pOtherWords) {
		MatchedItem thisMi = MatchedItem.createForReferredConceptPlural(this, pTgtCon, pConName, pOtherWords);

		saveMatchedItem(thisMi);
//		this.partialStartWord = true;
	}

	private void addReferredExactConceptPlural(ActionContext pAc, String pConName, CeConcept pTgtCon) {
		MatchedItem thisMi = MatchedItem.createForReferredConceptPlural(this, pTgtCon, pConName);

		saveMatchedItem(thisMi);

//		if (pTgtCon != null) {
//			if (this.referredExactConceptsPlural == null) {
//				this.referredExactConceptsPlural = new TreeMap<String, CeConcept>();
//			}
//
//			this.referredExactConceptsPlural.put(pConName, pTgtCon);
//		} else {
//			reportError("Unable to add exact concept plural '" + pConName + "' as it could not be located", pAc);
//		}
	}

	public Collection<CeConcept> listReferredExactConceptsPlural() {
		Collection<CeConcept> result = null;

		if (!getReferredExactConceptsPlural().isEmpty()) {
			result = getReferredExactConceptsPlural().values();
		} else {
			result = new ArrayList<CeConcept>();
		}

		return result;
	}

	private void addReferredExactConceptPastTenseFromMultipleWords(ActionContext pAc, String pConName, CeConcept pTgtCon, ArrayList<ProcessedWord> pOtherWords) {
		MatchedItem thisMi = MatchedItem.createForReferredConceptPastTense(this, pTgtCon, pConName, pOtherWords);

		saveMatchedItem(thisMi);
//		this.partialStartWord = true;
	}

	private void addReferredExactConceptPastTense(ActionContext pAc, String pConName, CeConcept pTgtCon) {
		MatchedItem thisMi = MatchedItem.createForReferredConceptPastTense(this, pTgtCon, pConName);

		saveMatchedItem(thisMi);
		
//		if (pTgtCon != null) {
//			if (this.referredExactConceptsPastTense == null) {
//				this.referredExactConceptsPastTense = new TreeMap<String, CeConcept>();
//			}
//
//			this.referredExactConceptsPastTense.put(pConName, pTgtCon);
//		} else {
//			reportError("Unable to add exact concept past tense '" + pConName + "' as it could not be located", pAc);
//		}
	}

	public Collection<CeConcept> listReferredExactConceptsPastTense() {
		Collection<CeConcept> result = null;

		if (!getReferredExactConceptsPastTense().isEmpty()) {
			result = getReferredExactConceptsPastTense().values();
		} else {
			result = new ArrayList<CeConcept>();
		}

		return result;
	}

	private void addReferredExactInstancesPlural(ActionContext pAc, String pInstName, CeInstance pTgtInst) {
		MatchedItem thisMi = MatchedItem.createForReferredInstancePlural(this, pTgtInst, pInstName);

		saveMatchedItem(thisMi);

//		if (pTgtInst != null) {
//			if (this.referredExactInstancesPlural == null) {
//				this.referredExactInstancesPlural = new TreeMap<String, CeInstance>();
//			}
//
//			// DSB 23/10/2015 - Metamodel instances should not be used
//			if (!WordCheckerCache.isOnlyConfigCon(pAc, pTgtInst)) {
//				this.referredExactInstancesPlural.put(pInstName, pTgtInst);
//			}
//		} else {
//			reportError("Unable to add exact instance plural '" + pInstName + "' as it could not be located", pAc);
//		}
	}

	public Collection<CeInstance> listReferredExactInstancesPlural() {
		Collection<CeInstance> result = null;

		if (!getReferredExactInstancesPlural().isEmpty()) {
			result = getReferredExactInstancesPlural().values();
		} else {
			result = new ArrayList<CeInstance>();
		}

		return result;
	}

	private void addReferredExactRelationFromMultipleWords(ActionContext pAc, String pPropFullName, CeProperty pTgtProp, ArrayList<ProcessedWord> pOtherWords) {
		MatchedItem thisMi = MatchedItem.createForReferredPropertyExact(this, pTgtProp, pPropFullName, pOtherWords);

		saveMatchedItem(thisMi);
//		this.partialStartWord = true;
	}

	private void addReferredExactRelation(ActionContext pAc, String pPropFullName, CeProperty pTgtProp) {
		MatchedItem thisMi = MatchedItem.createForReferredPropertyExact(this, pTgtProp, pPropFullName);

		saveMatchedItem(thisMi);

//		if (pTgtProp != null) {
//			if (this.referredExactRelations == null) {
//				this.referredExactRelations = new TreeMap<String, CeProperty>();
//			}
//
//			this.referredExactRelations.put(pPropFullName, pTgtProp);
//		} else {
//			reportError("Unable to add exact relation '" + pPropFullName + "' as it could not be located", pAc);
//		}
	}

	public TreeMap<String, ArrayList<MatchedItem>> getMatchedItemConceptMap() {
		TreeMap<String, ArrayList<MatchedItem>> conMap = new TreeMap<String, ArrayList<MatchedItem>>();

		for (MatchedItem mi : getMatchedItems()) {
			if (mi.getConcept() != null) {
				ArrayList<MatchedItem> conList = null;

				if (!conMap.containsKey(mi.getPhraseText())) {
					conList = new ArrayList<MatchedItem>();
					conMap.put(mi.getPhraseText(), conList);
				} else {
					conList = conMap.get(mi.getPhraseText());
				}

				conList.add(mi);
			}				
		}

		return conMap;
	}

	public TreeMap<String, ArrayList<MatchedItem>> getMatchedItemPropertyMap() {
		TreeMap<String, ArrayList<MatchedItem>> propMap = new TreeMap<String, ArrayList<MatchedItem>>();

		for (MatchedItem mi : getMatchedItems()) {
			if (mi.getProperty() != null) {
				ArrayList<MatchedItem> propList = null;

				if (!propMap.containsKey(mi.getPhraseText())) {
					propList = new ArrayList<MatchedItem>();
					propMap.put(mi.getPhraseText(), propList);
				} else {
					propList = propMap.get(mi.getPhraseText());
				}

				propList.add(mi);
			}				
		}

		return propMap;
	}

	public TreeMap<String, ArrayList<MatchedItem>> getMatchedItemInstanceMap() {
		TreeMap<String, ArrayList<MatchedItem>> instMap = new TreeMap<String, ArrayList<MatchedItem>>();

		for (MatchedItem mi : getMatchedItems()) {
			if (mi.getInstance() != null) {
				ArrayList<MatchedItem> instList = null;

				if (!instMap.containsKey(mi.getPhraseText())) {
					instList = new ArrayList<MatchedItem>();
					instMap.put(mi.getPhraseText(), instList);
				} else {
					instList = instMap.get(mi.getPhraseText());
				}

				instList.add(mi);
			}				
		}

		return instMap;
	}

	public void removeReferredRelation(String pKey) {
		MatchedItem itemToRemove = null;

		for (MatchedItem mi : this.matchedItems) {
			if (mi.isReferredPropertyExact()) {
				if (mi.getPhraseText().equals(pKey)) {
					itemToRemove = mi;
					break;
				}
			}
		}

		if (itemToRemove != null) {
			this.matchedItems.remove(itemToRemove);
		}

//		this.referredExactRelations.remove(pKey);
	}

	public void removeReferredConcept(String pKey) {
		MatchedItem itemToRemove = null;

		for (MatchedItem mi : this.matchedItems) {
			if (mi.getPhraseText().equals(pKey)) {
				if (mi.isReferredConceptExact()) {
					itemToRemove = mi;
					break;
				}
			}
		}

		if (itemToRemove != null) {
			this.matchedItems.remove(itemToRemove);
		}

//		this.referredExactConcepts.remove(pKey);
	}

	public void removeReferredInstance(String pKey) {
		MatchedItem itemToRemove = null;

		for (MatchedItem mi : this.matchedItems) {
			if (mi.getPhraseText().equals(pKey)) {
				if (mi.isReferredInstanceExact()) {
					itemToRemove = mi;
					break;
				}
			}
		}

		if (itemToRemove != null) {
			this.matchedItems.remove(itemToRemove);
		}

//		this.referredExactInstances.remove(pKey);
	}

	public Collection<CeProperty> listReferredExactRelations() {
		Collection<CeProperty> result = null;

		if (!getReferredExactRelations().isEmpty()) {
			result = getReferredExactRelations().values();
		} else {
			result = new ArrayList<CeProperty>();
		}

		return result;
	}

	private void checkForPartialMatchingConcepts(ActionContext pAc, String pLcWordText) {
		ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();
		CeConcept result = checkForPartialMatchingConcept(pAc, pLcWordText, true, otherWords);

		if (result != null) {
			if (result != getMatchingConcept()) {
				reportMicroDebug("Partial matched concept (" + result.getConceptName() + ") found for " + getWordText(),
						pAc);
				addReferredExactConceptFromMultipleWords(pAc, pLcWordText, result, otherWords);
			}
		}
	}

	private CeConcept checkForPartialMatchingConcept(ActionContext pAc, String pLcWordText, boolean pFirstTime, ArrayList<ProcessedWord> pOtherWords) {
		CeConcept result = null;
		String strippedWord = stripDelimitingQuotesFrom(pLcWordText);

		result = tryForPartialConceptMatchUsing(pAc, strippedWord, pFirstTime, pOtherWords);

		if (result == null) {
			String depluralised = depluralise(strippedWord);
			
			if (depluralised != null) {
				result = tryForPartialConceptMatchUsing(pAc, depluralised, pFirstTime, pOtherWords);
			}
		}

		return result;
	}

	private CeConcept tryForPartialConceptMatchUsing(ActionContext pAc, String pPossibleName, boolean pFirstTime, ArrayList<ProcessedWord> pOtherWords) {
		CeConcept result = null;

		if (pAc.getModelBuilder().isThereAConceptNameStartingButNotExactly(pAc, pPossibleName)) {
			ProcessedWord nextWord = getNextProcessedWord();

			reportMicroDebug("Partial match for concept '" + pPossibleName + "'", pAc);

			if (nextWord != null) {
				String concatLcText = pPossibleName + " " + getNextProcessedWord().getDeclutteredText();
				result = nextWord.checkForPartialMatchingConcept(pAc, concatLcText, false, pOtherWords);
			}

			if (result != null) {
//				nextWord.partialConceptReference = true;
				pOtherWords.add(nextWord);
			}
		} else {
			result = pAc.getModelBuilder().getConceptNamed(pAc, pPossibleName);

			if (!pFirstTime && (result == null)) {
				reportMicroDebug("No further partial match for concept '" + pPossibleName + "'", pAc);
			}
		}

		// If the test failed retry with just the passed name
		if (result == null) {
			result = pAc.getModelBuilder().getConceptNamed(pAc, pPossibleName);
		}
		
		return result;
	}

	private boolean alreadyMatchesRelation(CeProperty pProp) {
		boolean result = false;

		if (matchesToRelations()) {
			result = getMatchingRelations().containsValue(pProp);
		} else {
			result = false;
		}

		return result;
	}

	private void checkForPartialMatchingRelations(ActionContext pAc, String pLcWordText) {
		ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();
		TreeMap<String, ArrayList<CeProperty>> result = checkForPartialMatchingRelation(pAc, pLcWordText, true, otherWords);

		if (result != null) {
			for (String thisKey : result.keySet()) {
				ArrayList<CeProperty> propList = result.get(thisKey);

				for (CeProperty thisProp : propList) {
					if (!alreadyMatchesRelation(thisProp)) {
						reportMicroDebug(
								"Partial matched relation (" + thisProp.getPropertyName() + ") found for " + getWordText(),
								pAc);
						addReferredExactRelationFromMultipleWords(pAc, thisKey, thisProp, otherWords);
					}
				}
			}
		}
	}

	private TreeMap<String, ArrayList<CeProperty>> checkForPartialMatchingRelation(ActionContext pAc, String pLcWordText,
			boolean pFirstTime, ArrayList<ProcessedWord> pOtherWords) {
		TreeMap<String, ArrayList<CeProperty>> result = null;

		if (pAc.getModelBuilder().isThereADefinedPropertyNameStartingButNotExactly(pLcWordText)) {
			ProcessedWord nextWord = getNextProcessedWord();

			reportMicroDebug("Partial match for relation '" + pLcWordText + "'", pAc);

			if (nextWord != null) {
				String concatLcText = pLcWordText + " " + getNextProcessedWord().getDeclutteredText();
				result = nextWord.checkForPartialMatchingRelation(pAc, concatLcText, false, pOtherWords);
			}
		} else {
			if (!pFirstTime) {
				// DSB 23/10/2015 - Improved to only consider non-inferred
				// properties
				ArrayList<CeProperty> tempResult = pAc.getModelBuilder().getPropertiesNamed(pLcWordText);

				for (CeProperty thisProp : tempResult) {
					CeProperty rootProp = null;

					if (thisProp.isInferredProperty()) {
						rootProp = thisProp.getStatedSourceProperty();
					} else {
						rootProp = thisProp;
					}

					if (result == null) {
						result = new TreeMap<String, ArrayList<CeProperty>>();
					}

					if (result.get(pLcWordText) == null) {
						result.put(pLcWordText, new ArrayList<CeProperty>());
					}

					if (!result.get(pLcWordText).contains(rootProp)) {
						result.get(pLcWordText).add(rootProp);
					}
				}

				if (result == null) {
					reportMicroDebug("No further partial match for property '" + pLcWordText + "'", pAc);
				}
			}
		}

		if (!pFirstTime) {
			// If the test failed retry with just the passed name
			if ((result == null) || (result.isEmpty())) {
				result = new TreeMap<String, ArrayList<CeProperty>>();
				result.put(pLcWordText, pAc.getModelBuilder().getPropertiesNamed(pLcWordText));
			}
		}

		if ((result != null) && (!result.isEmpty()) && (result.size() > 0)) {
//			this.partialRelationReference = true;
			if (!pFirstTime) {
				pOtherWords.add(this);
			}
		}

		return result;
	}

	private void addReferredExactInstanceFromMultipleWords(String pKey, CeInstance pInst, ArrayList<ProcessedWord> pOtherWords) {
		MatchedItem thisMi = MatchedItem.createForReferredInstanceExact(this, pInst, pKey, pOtherWords);

		saveMatchedItem(thisMi);
//		this.partialStartWord = true;
	}

	private void addReferredExactInstance(String pKey, CeInstance pInst) {
		MatchedItem thisMi = MatchedItem.createForReferredInstanceExact(this, pInst, pKey);

		saveMatchedItem(thisMi);

//		if (this.referredExactInstances == null) {
//			this.referredExactInstances = new TreeMap<String, CeInstance>();
//		}
//
//		this.referredExactInstances.put(pKey, pInst);
	}

	public Collection<CeInstance> listReferredExactInstances() {
		Collection<CeInstance> result = null;

		if (!getReferredExactInstances().isEmpty()) {
			result = getReferredExactInstances().values();
		} else {
			result = new ArrayList<CeInstance>();
		}

		return result;
	}

	private void checkForPartialMatchingInstances(ActionContext pAc, String pLcWordText) {
		ArrayList<ProcessedWord> otherWords = new ArrayList<ProcessedWord>();
		TreeMap<String, ArrayList<CeInstance>> result = checkForPartialMatchingInstanceList(pAc, pLcWordText, true, otherWords);

		if (result != null) {
			for (String phraseText : result.keySet()) {
				for (CeInstance thisInst : result.get(phraseText)) {
					if (!getMatchingInstances().contains(thisInst)) {
						reportMicroDebug(
								"Partial matched instance (" + thisInst.getInstanceName() + ") found for " + phraseText,
								pAc);
//						this.partialInstanceReference = true;
	
						addReferredExactInstanceFromMultipleWords(phraseText, thisInst, otherWords);
					}
				}
			}
		}
	}

	private TreeMap<String, ArrayList<CeInstance>> checkForPartialMatchingInstanceList(ActionContext pAc, String pLcWordText, boolean pFirstTime, ArrayList<ProcessedWord> pOtherWords) {
		TreeMap<String, ArrayList<CeInstance>> result = null;
		HudsonManager hm = ServletStateManager.getHudsonManager(pAc);
		String strippedWord = stripDelimitingQuotesFrom(pLcWordText);

		ArrayList<CeInstance> possInsts = hm.getIndexedEntityAccessor(pAc.getModelBuilder())
				.calculateInstancesWithNameStarting(pAc, strippedWord + " ");

		if (!possInsts.isEmpty()) {
			ProcessedWord nextWord = getNextProcessedWord();

			reportMicroDebug("Partial match for instance '" + strippedWord + "'", pAc);

			if (nextWord != null) {
				String concatLcText = pLcWordText + " " + getNextProcessedWord().getDeclutteredText();

				// Quick and dirty depluralisation (to stop the need to specify
				// lots of singular variants of the
				// word as synonyms. Won't work for complex plurals like
				// "people"...
				if (concatLcText.endsWith("s")) {
					ArrayList<String> variants = new ArrayList<String>();
					variants.add(concatLcText);
					variants.add(concatLcText.substring(0, (concatLcText.length() - 1)));

					result = new TreeMap<String, ArrayList<CeInstance>>();

					for (String thisVariant : variants) {
						TreeMap<String, ArrayList<CeInstance>> matches = nextWord.checkForPartialMatchingInstanceList(pAc, thisVariant, false, pOtherWords);

						if (matches != null) {
							for (String thisKey : matches.keySet()) {
								ArrayList<CeInstance> instList = matches.get(thisKey);
								result.put(thisKey, instList);
							}
						}
					}
				} else {
					result = nextWord.checkForPartialMatchingInstanceList(pAc, concatLcText, false, pOtherWords);
				}

				if ((result != null) && (!result.isEmpty())) {
					pOtherWords.add(nextWord);
//					nextWord.partialInstanceReference = true;
//					nextWord.partialStartWord = false;
				}
			}
		}

		if (result == null) {
			result = new TreeMap<String, ArrayList<CeInstance>>();
		}

		// DSB 20/10/2015 - Moved this processing out so it is always done (even
		// when there
		// are partial matches
		ArrayList<CeInstance> extraResult = hm.getWordCheckerCache().checkForMatchingInstances(pAc, strippedWord);

		if (!extraResult.isEmpty()) {
			result.put(strippedWord, extraResult);
		}

		if (!pFirstTime && (result.isEmpty())) {
			reportMicroDebug("No further partial match for instance '" + strippedWord + "'", pAc);
		}

		return result;
	}

	private void checkForPluralMatchingInstance(ActionContext pAc, String pLcWordText, WordCheckerCache pWcc) {
		String strippedWord = stripDelimitingQuotesFrom(pLcWordText);

		// First check for the simple "extra s" case
		if (strippedWord.endsWith("s")) {
			String singForm = strippedWord.substring(0, (strippedWord.length() - 1));
			CeInstance matchedInst = pAc.getModelBuilder().getInstanceNamed(pAc, singForm);

			if (matchedInst != null) {
				addReferredExactInstancesPlural(pAc, singForm, matchedInst);
			}
		}

		for (CeInstance lingInst : pWcc.getLingThingPluralForms(pAc, strippedWord)) {
			addReferredExactInstancesPlural(pAc, strippedWord, lingInst);
		}

	}

	public ProcessedWord getNextProcessedWord() {
		ProcessedWord result = null;
		ConvWord nextConvWord = this.convWord.getNextWord();

		if (nextConvWord != null) {
			result = nextConvWord.getProcessedWord();
		}

		return result;
	}

	public ProcessedWord getPreviousProcessedWord() {
		ProcessedWord result = null;
		ConvWord prevConvWord = this.convWord.getPreviousWord();

		if (prevConvWord != null) {
			result = prevConvWord.getProcessedWord();
		}

		return result;
	}

	public boolean isGrounded() {
		return isGroundedOnConcept() || isGroundedOnProperty() || isGroundedOnInstance();
	}

	public boolean isValidSubjectWord(ActionContext pAc) {
		boolean result = false;

		// Exclude any words that only have attributional thing instances linked
		if (isGroundedOnInstance()) {
			for (CeInstance thisInst : listGroundedInstances()) {
				if (thisInst.getDirectConcepts().length > 1) {
					result = true;
				} else {
					// TODO: Remove this hardcoded name
					if (!thisInst.isConceptNamed(pAc, "attributional thing")) {
						result = true;
					}
				}
			}
		} else {
			result = true;
		}

		return result;
	}

	public boolean isGroundedOnConcept() {
//		return matchesToConcept() || refersToConceptsExactly() || refersToPluralConceptsExactly() || this.partialConceptReference;
		return matchesToConcept() || refersToConceptsExactly() || refersToPluralConceptsExactly();
	}

	public boolean isGroundedOnProperty() {
//		return matchesToRelations() || refersToRelationsExactly() || this.partialRelationReference;
		return matchesToRelations() || refersToRelationsExactly();
	}

	public boolean isGroundedOnInstance() {
//		return (matchesToInstance() && !isLaterPartOfPartial()) || refersToInstancesExactly()
//				|| refersToPluralInstancesExactly() || (this.partialInstanceReference && this.partialStartWord);
//		return matchesToInstance() || refersToInstancesExactly() || refersToPluralInstancesExactly() || (this.partialInstanceReference && this.partialStartWord);
		return matchesToInstance() || refersToInstancesExactly() || refersToPluralInstancesExactly();
	}

	public ArrayList<CeConcept> listGroundedConcepts() {
		ArrayList<CeConcept> result = null;

//		if (this.partialConceptReference) {
//			result = getPreviousProcessedWord().listGroundedConcepts();
//		} else {
			HashSet<CeConcept> set = new HashSet<CeConcept>();

			if (getMatchingConcept() != null) {
				set.add(getMatchingConcept());
			}

			if (refersToConceptsExactly()) {
				set.addAll(getReferredExactConcepts().values());
			}

			if (refersToPluralConceptsExactly()) {
				set.addAll(getReferredExactConceptsPlural().values());
			}

			if (refersToPastTenseConceptsExactly()) {
				set.addAll(getReferredExactConceptsPastTense().values());
			}

//			result = winnowConcepts(set);
			result = new ArrayList<CeConcept>(set);
//		}

		return result;
	}

	public TreeMap<String, ArrayList<CeConcept>> listGroundedConceptsAndKeys() {
		TreeMap<String, ArrayList<CeConcept>> result = new TreeMap<String, ArrayList<CeConcept>>();

		ArrayList<CeConcept> conList = null;

		if (getMatchingConcept() != null) {
			if (result.get(getWordText()) == null) {
				conList = new ArrayList<CeConcept>();
				result.put(getWordText(), conList);
			} else {
				conList = result.get(getWordText());
			}
			conList.add(getMatchingConcept());
		}

		if (getReferredExactConcepts() != null) {
			for (String thisKey : getReferredExactConcepts().keySet()) {
				CeConcept thisCon = getReferredExactConcepts().get(thisKey);
				if (result.get(thisKey) == null) {
					conList = new ArrayList<CeConcept>();
					result.put(thisKey, conList);
				} else {
					conList = result.get(thisKey);
				}
				conList.add(thisCon);
			}
		}

		for (String thisKey : getReferredExactConceptsPlural().keySet()) {
			CeConcept thisCon = getReferredExactConceptsPlural().get(thisKey);
			if (result.get(thisKey) == null) {
				conList = new ArrayList<CeConcept>();
				result.put(thisKey, conList);
			} else {
				conList = result.get(thisKey);
			}
			conList.add(thisCon);
		}

		for (String thisKey : getReferredExactConceptsPastTense().keySet()) {
			CeConcept thisCon = getReferredExactConceptsPastTense().get(thisKey);
			if (result.get(thisKey) != null) {
				conList = new ArrayList<CeConcept>();
				result.put(thisKey, conList);
			} else {
				conList = result.get(thisKey);
			}
			conList.add(thisCon);
		}

		return result;
	}

	public ArrayList<CeConcept> listGroundedConceptsNoPlural() {
		ArrayList<CeConcept> result = null;

//		if (this.partialConceptReference) {
//			result = getPreviousProcessedWord().listGroundedConcepts();
//		} else {
			HashSet<CeConcept> set = new HashSet<CeConcept>();

			if (getMatchingConcept() != null) {
				set.add(getMatchingConcept());
			}

			if (refersToConceptsExactly()) {
				set.addAll(getReferredExactConcepts().values());
			}

//			result = winnowConcepts(set);
			result = new ArrayList<CeConcept>(set);
//		}

		return result;
	}

	public ArrayList<CeProperty> listGroundedProperties() {
		HashSet<CeProperty> result = new HashSet<CeProperty>();

		if (getMatchingRelations() != null) {
			result.addAll(getMatchingRelations().values());
		}

		if (refersToRelationsExactly()) {
			result.addAll(getReferredExactRelations().values());
		}

//		return winnowProperties(result);
		return new ArrayList<CeProperty>(result);
	}

	public TreeMap<String, ArrayList<CeProperty>> listGroundedPropertiesAndKeys() {
		TreeMap<String, ArrayList<CeProperty>> result = new TreeMap<String, ArrayList<CeProperty>>();
		ArrayList<CeProperty> propList = null;

		if (getMatchingRelations() != null) {
			for (String thisKey : getMatchingRelations().keySet()) {
				CeProperty thisProp = getMatchingRelations().get(thisKey);
				if (result.get(thisKey) == null) {
					propList = new ArrayList<CeProperty>();
					result.put(thisKey, propList);
				} else {
					propList = result.get(thisKey);
				}
				propList.add(thisProp);
			}
		}

		for (String thisKey : getReferredExactRelations().keySet()) {
			CeProperty thisProp = getReferredExactRelations().get(thisKey);
			if (result.get(thisKey) == null) {
				propList = new ArrayList<CeProperty>();
				result.put(thisKey, propList);
			} else {
				propList = result.get(thisKey);
			}
			propList.add(thisProp);
		}

		return result;
	}

	public ArrayList<CeInstance> listGroundedInstances() {
		ArrayList<CeInstance> result = new ArrayList<CeInstance>();
//		if (this.partialConceptReference) {
//			result = getPreviousProcessedWord().listGroundedInstances();
//		} else {
			HashSet<CeInstance> set = new HashSet<CeInstance>();

//			if (!this.partialInstanceReference) {
				if (!getMatchingInstances().isEmpty()) {
					set.addAll(getMatchingInstances());
				}
//			}

			if (refersToInstancesExactly()) {
				set.addAll(getReferredExactInstances().values());
			}

			if (refersToPluralInstancesExactly()) {
				set.addAll(getReferredExactInstancesPlural().values());
			}

//			result = winnowInstances(set);
			result = new ArrayList<CeInstance>(set);
//		}

		return result;
	}

	public TreeMap<String, ArrayList<CeInstance>> listGroundedInstancesAndKeys() {
		TreeMap<String, ArrayList<CeInstance>> result = new TreeMap<String, ArrayList<CeInstance>>();
		ArrayList<CeInstance> instList = null;
		
		for (CeInstance thisInst : getMatchingInstances()) {
			if (result.get(getWordText()) == null) {
				instList = new ArrayList<CeInstance>();
				result.put(getWordText(), instList);
			} else {
				instList = result.get(getWordText());
			}
			instList.add(thisInst);
		}

		for (String thisKey : getReferredExactInstances().keySet()) {
			if (result.get(thisKey) == null) {
				instList = new ArrayList<CeInstance>();
				result.put(thisKey, instList);
			} else {
				instList = result.get(thisKey);
			}
			instList.add(getReferredExactInstances().get(thisKey));
		}

		for (String thisKey : getReferredExactInstancesPlural().keySet()) {
			if (result.get(thisKey) == null) {
				instList = new ArrayList<CeInstance>();
				result.put(thisKey, instList);
			} else {
				instList = result.get(thisKey);
			}
			instList.add(this.getReferredExactInstancesPlural().get(thisKey));
		}

		return result;
	}

	public ArrayList<ProcessedWord> getFollowingUnprocessedWords() {
		ArrayList<ProcessedWord> result = new ArrayList<ProcessedWord>();

		ProcessedWord nw = getNextProcessedWord();

		if (nw != null) {
			if (nw.isUnmatchedWord()) {
				result.add(nw);
				result.addAll(nw.getFollowingUnprocessedWords());
			}
		}

		return result;
	}

	public boolean isModifier(ActionContext pAc) {
		boolean result = false;

		for (CeInstance thisInst : listGroundedInstances()) {
			if (thisInst.isConceptNamed(pAc, GenericHandler.CON_MODIFIER)) {
				result = true;
				break;
			}
		}

		return result;
	}

	public boolean isSearchModifier(ActionContext pAc) {
		boolean result = false;

		for (CeInstance thisInst : listGroundedInstances()) {
			if (thisInst.isConceptNamed(pAc, GenericHandler.CON_SRCHMOD)) {
				result = true;
				break;
			}
		}

		return result;
	}

	public boolean isFilterModifier(ActionContext pAc) {
		boolean result = false;

		for (CeInstance thisInst : listGroundedInstances()) {
			if (thisInst.isConceptNamed(pAc, GenericHandler.CON_FILTMOD)) {
				result = true;
				break;
			}
		}

		return result;
	}

	public boolean isFunctionModifier(ActionContext pAc) {
		boolean result = false;

		for (CeInstance thisInst : listGroundedInstances()) {
			if (thisInst.isConceptNamed(pAc, GenericHandler.CON_FUNCMOD)) {
				result = true;
				break;
			}
		}

		return result;
	}

	@Override
	public String toString() {
		String result = getWordText();

		if (matchesToConcept()) {
			result += " (corresponds to concept: " + getMatchingConcept().getConceptName() + ")";
		}

		if (matchesToRelations()) {
			boolean firstTime = true;
			result += " (corresponds to relation: ";

			for (CeProperty thisProp : getMatchingRelations().values()) {
				if (!firstTime) {
					result += ", ";
				}

				result += thisProp.formattedFullPropertyName();
				firstTime = false;
			}

			result += ")";
		}

		if (matchesToInstance()) {
			boolean firstTime = true;
			result += " (corresponds to instance of: ";

			for (CeInstance thisInst : getMatchingInstances()) {
				if (!firstTime) {
					result += ", ";
				}

				result += thisInst.formattedDirectConceptNames();
				firstTime = false;
			}

			result += ")";
		}

		if (refersToConceptsExactly()) {
			String refConText = "";
			for (CeConcept refCon : getReferredExactConcepts().values()) {
				if (!refConText.isEmpty()) {
					refConText += ", ";
				}
				refConText += refCon.getConceptName();
			}

			refConText = " (refers to concept: " + refConText + ")";
			result += refConText;
		}

		if (refersToPluralConceptsExactly()) {
			String refConText = "";
			for (CeConcept refCon : getReferredExactConceptsPlural().values()) {
				if (!refConText.isEmpty()) {
					refConText += ", ";
				}
				refConText += refCon.getConceptName();
			}

			refConText = " (refers to concept plural: " + refConText + ")";
			result += refConText;
		}

		if (refersToPastTenseConceptsExactly()) {
			String refConText = "";
			for (CeConcept refCon : getReferredExactConceptsPastTense().values()) {
				if (!refConText.isEmpty()) {
					refConText += ", ";
				}
				refConText += refCon.getConceptName();
			}

			refConText = " (refers to concept past tense: " + refConText + ")";
			result += refConText;
		}

		if (refersToRelationsExactly()) {
			String refPropText = "";
			for (CeProperty refProp : getReferredExactRelations().values()) {
				if (!refPropText.isEmpty()) {
					refPropText += ", ";
				}
				refPropText += refProp.formattedFullPropertyName();
			}

			refPropText = " (refers to relation: " + refPropText + ")";
			result += refPropText;
		}

		if (refersToInstancesExactly()) {
			String refInstText = "";
			for (CeInstance refInst : getReferredExactInstances().values()) {
				if (!refInstText.isEmpty()) {
					refInstText += ", ";
				}
				refInstText += refInst.getInstanceName();
			}

			refInstText = " (refers to instance: " + refInstText + ")";
			result += refInstText;
		}

		if (refersToPluralInstancesExactly()) {
			String refInstText = "";
			for (CeInstance refInst : getReferredExactInstancesPlural().values()) {
				if (!refInstText.isEmpty()) {
					refInstText += ", ";
				}
				refInstText += refInst.getInstanceName();
			}

			refInstText = " (refers to instance plural: " + refInstText + ")";
			result += refInstText;
		}

//		if (this.isQuestionWord) {
//			result += " (question word)";
//		}

//		if (this.isStandardWord) {
//			result += " (standard word)";
//		}

//		if (this.isNegationWord) {
//			result += " (negation word)";
//		}

//		if (this.isValueWord) {
//			result += " (value word)";
//		}

		if (this.isNumberWord) {
			result += " (number word)";
		}

//		if (this.partialInstanceReference) {
//			result += " (partial instance reference)";
//		}

//		if (this.partialConceptReference) {
//			result += " (partial concept reference)";
//		}

//		if (this.partialRelationReference) {
//			result += " (partial relation reference)";
//		}

		return result;
	}

//	public CeInstance getChosenInstance() {
//	return this.chosenInstance;
//}

//public String getConceptName() {
//	String result = null;
//
//	if (isUnmatchedWord()) {
//		result = CON_UNMWORD;
//	} else {
//		result = CON_PROWORD;
//	}
//
//	return result;
//}

//public String getDeterminer() {
//	String result = null;
//
//	if (isUnmatchedWord()) {
//		result = DET_AN;
//	} else {
//		result = DET_A;
//	}
//
//	return result;
//}

//public void setChosenInstance(CeInstance pInst) {
//	this.chosenInstance = pInst;
//}

//	public String getWholePhraseText() {
//	String result = null;
//
//	result = getWordText();
//
////	if (this.partialStartWord) {
////		ProcessedWord nextWord = getNextProcessedWord();
////
////		if (nextWord != null) {
////			result += nextWord.getFollowingPartialText();
////		}
////	}
//
//	return result;
//}

//public String getFollowingPartialText() {
//	String result = null;
//
//	if (this.isLaterPartOfPartial()) {
//		result = " " + getWordText();
//
//		ProcessedWord nextWord = getNextProcessedWord();
//
//		if (nextWord != null) {
//			result += nextWord.getFollowingPartialText();
//		}
//	} else {
//		result = "";
//	}
//
//	return result;
//}

//public CeConcept getMatchingConcept() {
//	return this.matchingConcept;
//}

//	public ArrayList<CeInstance> getMatchingInstances() {
//	return this.matchingInstances;
//}

//	public ArrayList<ExtractedItem> getExtractedItems() {
//		return this.extractedItems;
//	}

//	public void addExtractedItem(ExtractedItem pEi) {
//		if (this.extractedItems == null) {
//			this.extractedItems = new ArrayList<ExtractedItem>();
//		}
//
//		this.extractedItems.add(pEi);
//	}

//	public boolean isStandardWord() {
//		return this.isStandardWord;
//	}

//	public boolean isNegationWord() {
//		return this.isNegationWord;
//	}

//	public boolean isPureNegation() {
////		return this.isNegationWord && (!isGrounded() && !this.partialStartWord);
//		return this.isNegationWord && !isGrounded();
//	}

//	public boolean isWhat() {
//		return this.lcText.equals(Q_WHAT);
//	}

//	public boolean isWho() {
//		return this.lcText.equals(Q_WHO);
//	}

//	public boolean isWhere() {
//		return this.lcText.equals(Q_WHERE);
//	}

//	public boolean isWhich() {
//		return this.lcText.equals(Q_WHICH);
//	}

//	public boolean isWhy() {
//		return this.lcText.equals(Q_WHY);
//	}

//	public boolean isCount() {
//		return this.lcText.equals(Q_COUNT);
//	}

//	public boolean isList() {
//		return this.lcText.equals(Q_LIST);
//	}

//	public boolean isSummarise() {
//		return this.lcText.equals(Q_SUMM1) || this.lcText.equals(Q_SUMM2);
//	}

//	public boolean isAll() {
//		return this.lcText.equals(Q_ALL);
//	}

//	public boolean isValueWord() {
//		return this.isValueWord;
//	}

//	public boolean isQuestionWord() {
//	return this.isQuestionWord;
//}

//public void markAsQuestionWord() {
//	this.isQuestionWord = true;
//}

//public boolean isDeterminer() {
//	String decText = getDeclutteredText();
//
//	// TODO: This should not be hardcoded
//	return (decText.equals(DET_A)) || (decText.equals(DET_AN)) || (decText.equals(DET_THE));
//}

//	public boolean isLaterPartOfPartial() {
////		return (this.partialConceptReference || this.partialRelationReference || this.partialInstanceReference)
////				&& !this.partialStartWord;
//		return (this.partialConceptReference || this.partialRelationReference || this.partialInstanceReference);
//	}

//	public TreeMap<String, CeProperty> getMatchingRelations() {
//	return this.matchingRelations;
//}

//	public void markWordAsValue() {
//		this.isValueWord = true;
//	}

//	public void markWordAsNumber() {
//		this.isNumberWord = true;
//	}

//	private TreeMap<String, CeConcept> getReferredExactConcepts() {
//		return this.referredExactConcepts;
//	}

//	private void checkForStandardWords(ConvConfig pCc, WordCheckerCache pWcc, ActionContext pAc) {
//		String myText = getDeclutteredText();
//
////		ArrayList<String> cWords = pWcc.getCommonWords(pCc, pAc);
////		ArrayList<String> nWords = pWcc.getNegationWords(pCc, pAc);
//
////		if ((cWords != null) && (cWords.contains(myText))) {
////			this.isStandardWord = true;
////		}
//
////		if ((nWords != null) && (nWords.contains(myText))) {
////			this.isNegationWord = true;
////		}
//	}

//	private static ArrayList<CeConcept> winnowConcepts(HashSet<CeConcept> pCons) {
//		ArrayList<CeConcept> result = new ArrayList<CeConcept>();
//
//		for (CeConcept thisCon : pCons) {
//			if (!hasChildConceptIn(thisCon, pCons)) {
//				result.add(thisCon);
//			}
//		}
//
//		return result;
//	}

//	private static ArrayList<CeProperty> winnowProperties(HashSet<CeProperty> pProps) {
//		ArrayList<CeProperty> result = new ArrayList<CeProperty>();
//
//		// TODO: Implement this
//		for (CeProperty thisProp : pProps) {
//			result.add(thisProp);
//		}
//
//		return result;
//	}

//	private static ArrayList<CeInstance> winnowInstances(HashSet<CeInstance> pInsts) {
//		ArrayList<CeInstance> result = new ArrayList<CeInstance>();
//
//		// Instances cannot be winnowed
//		for (CeInstance thisInst : pInsts) {
//			result.add(thisInst);
//		}
//
//		return result;
//	}

//	private static boolean hasChildConceptIn(CeConcept tgtCon, HashSet<CeConcept> pCandidates) {
//		for (CeConcept thisCand : pCandidates) {
//			if (tgtCon.isParentOf(thisCand)) {
//				return true;
//			}
//		}
//
//		return false;
//	}

//	public void addConnectedWordsTo(ExtractedItem pExtItem) {
//		if (this.partialStartWord) {
//			ProcessedWord nw = getNextProcessedWord();
//
//			if ((nw != null) && (nw.partialConceptReference)) {
//				pExtItem.addOtherWord(nw);
//				nw.addConnectedWordsTo(pExtItem);
//			}
//
//			if ((nw != null) && (nw.partialRelationReference)) {
//				pExtItem.addOtherWord(nw);
//				nw.addConnectedWordsTo(pExtItem);
//			}
//
//			if ((nw != null) && (nw.partialInstanceReference)) {
//				pExtItem.addOtherWord(nw);
//				nw.addConnectedWordsTo(pExtItem);
//			}
//		}
//	}

}
