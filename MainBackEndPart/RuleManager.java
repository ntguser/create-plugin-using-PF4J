package com.ntg.common.RuleEngine;

import com.ntg.Smart2Go.Application;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import com.ntg.Smart2Go.utils.IUDAType;
import com.ntg.Smart2Go.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.ntg.Smart2Go.GenericPluginInterfaceImp;
import com.ntg.Smart2Go.controllers.internal.controllers.RuleBuilderController;
import com.ntg.Smart2Go.internal.doa.PluginRepository;
import com.ntg.Smart2Go.internal.dto.Attachment;
import com.ntg.Smart2Go.internal.dto.GenericObject;
import com.ntg.Smart2Go.internal.dto.UDAsWithValues;
import com.ntg.Smart2Go.internal.entites.RuleBuilder;
import com.ntg.Smart2Go.internal.entites.RuleDBMapping;
import com.ntg.Smart2Go.internal.entites.RuleStaticMapping;
import com.ntg.Smart2Go.internal.entites.TypesUDa;
import com.ntg.Smart2Go.internal.entites.UdaMultiValue;
import com.ntg.Smart2Go.internal.services.TypesUDAsService;
import com.ntg.Smart2Go.internal.services.rules.RuleExecutionResult;
import com.ntg.Smart2Go.plugin.config.SpringConfiguration;
import com.ntg.Smart2Go.utils.exceptions.NTGException;
import com.ntg.Smart2Go.utils.exceptions.PreventSaveException;
import com.ntg.common.NTGMessageOperation;
import com.ntg.common.STAGESystemOut;
import net.bull.javamelody.MonitoredWithSpring;
/*********************************
 * Copyright (c) 2017 Aalmalky to Present. All right reserved
 **********************************/

@Component
public class RuleManager {

    @Autowired
    RuleExecutionResult _ruleExecutionResult;

    @Autowired
    private SLAEngine slaEngine;

    @Autowired
    private ActionWebService actionWebService;

    @Autowired
    private ActionDBMapping dbMapping;

    @Autowired
    private ActionRemoteDBMapping remoteDBMapping;

    @Autowired
    private PushingDataIntoAnotherObject pushingDataIntoObject;

    @Autowired
    private ActionStaticMapping staticMapping;

    @Autowired
    private ActionPushTemplate actionPushTemplate;

    @Autowired
    private RuleLogger ruleLogger;

    @Autowired
    private ActionCopyPlanTemplate copyPlanTemplate;

    @Autowired
    private ActionMessageQueue messageQueue;

    @Autowired
    RuleHandler ruleHandler;

    @Autowired
    RuleValidator ruleValidator;

    @Autowired
    ActionExecuteSQL actionExecuteSQL;

    @Autowired
    private TypesUDAsService typesUdaService;
    
    @Autowired
	PluginRepository PluginRepo;

    @Autowired
    ActionJsRule actionJsRule;

    /**
     * List of engine actions
     */
    public static final int PREVENT_SAVE = 0;
    public static final int WEB_SERVICE_CALL = 1;
    public static final int SLA = 2;
    public static final int FORM_MAPPING = 3;
    public static final int DB_INVOKER = 4;
    public static final int PUSH_TEMPLATE_DB = 5;
    public static final int STATIC_MAPPING = 6;
    public static final int PUSH_TEMPLATE = 7;
    public static final int PUSH_DATA_TO_OTHER_OBJECT = 8;
    public static final int NO_ACTION = 10;
    public static final int EXCUTE_SQL = 14;
    public static final int EXIT_ACTION = 15;
    public static final int COPY_PLAN_TEMPLATE = 18;
    public static final int MESSAGE_QUEUE = 21;
    public static final int JS_RULE = 22;
    public static final int RELOAD_CHARTS = 23;
    public static final int ShortCut_Navigation = 24;
    public static final int EXPORT_ACTION = 25;
    public static final int Plugin = 26;

        List myList = new ArrayList();
    /**
     * for performance sake This method expected rules as tree structure from the
     * caller
     *
     * @param rules
     * @param parentId
     * @param object
     * @param oldObject
     * @param objectRecId
     * @param ruleType
     * @param RULE_NUMBER
     * @param isNew
     * @param newUdas
     * @param oldUdas
     * @param gridRowIndex 
     * @throws NTGException
     * @throws Exception
     */
    @MonitoredWithSpring
    @Transactional
    public void executeRules(List<RuleBuilder> rules, long parentId, GenericObject object, Object oldObject,
                             long objectRecId, String ruleType, int RULE_NUMBER, boolean isNew, Map<String, UDAsWithValues> newUdas,
                             Map<String, UDAsWithValues> oldUdas,
                             Map<Long, RuleStaticMapping> staticMappingRulesMap, Map<Long, RuleDBMapping> dbMappingRulesMap,
                             boolean isOnLoadCreate, String DoneAction, boolean isGridInvoker, Map<String, Object> gridRow, boolean plSqlAfterAction, 
                             Integer gridRowIndex) throws PreventSaveException, NTGException, Exception {
            myList = new ArrayList();
        RULE_NUMBER++;
        String currentRule = null;
        Set<Long> ignored = new HashSet<Long>();
        Long exitActionVar = 0L;
        //@samaa Eissa Dev-00002616:Rules --> rules doesn't execute  with sequence for parents and child
        if(!plSqlAfterAction) {
            rules=sortRulesOrder(rules);
        }
        //////////////////////////////////
        for (RuleBuilder rule : rules) {
            if (rule.getDisabled() == false) {
                if (exitActionVar.equals(rule.getParentId()))
                    continue;

                if (_ruleExecutionResult.exitFromAllRules < 0 || _ruleExecutionResult.RULE_ACTION_STATUS == 1)
                    return;

                if (ignored.contains(rule.getParentId()))
                    ignored.add(rule.getRecId());

                clearPreventSaveValues(object);

                if (isValidBackendRule(rule, objectRecId, ruleType, isNew) && !ignored.contains(rule.getRecId())) {
                    try {
                        currentRule = ruleHandler.ruleCondition(object, rule.getrule(),oldObject, DoneAction, isGridInvoker, gridRow,true);
                        // added to handle if index of is in rule
                        if (currentRule != null && currentRule.contains(".indexOf")) {
                            if (currentRule.contains("''")) {
                                currentRule = currentRule.replace("''", "'");
                            }
                            if (currentRule.contains("','") && !currentRule.contains(".indexOf(',')")) {
                                currentRule = currentRule.replace("','", ",");
                            }
                        }
                        if (currentRule != null
                                && (currentRule.contains("subtract(") || currentRule.contains("subtract ("))) {
                            currentRule = currentRule.replace("subtract (", "subtract(");
                            String str = currentRule.substring(currentRule.indexOf("subtract("),
                                    currentRule.indexOf(")") + 1);
                            String dates = currentRule.substring(currentRule.indexOf("subtract(") + 9,
                                    currentRule.indexOf(")"));
                            String[] ruleStr = dates.split(" - ");
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            Date date1 = sdf.parse(ruleStr[0].replace("'", ""));
                            Date date2 = sdf.parse(ruleStr[1].replace("'", ""));

                            Long diffInMillies = Math.abs(date1.getTime() - date2.getTime());
                            Long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);
                            currentRule = currentRule.replace(str, diff.toString());
                        }
                        // ---------------------------------------------------------------
                        if (ruleValidator.isRuleValid(currentRule)) {

                            switch (rule.getRULE_ACTION()) {
                                case PREVENT_SAVE:
                                    // prevent save
                                    preventObjectSave(object, rule.getUserMessage(), rule, ruleType,
                                            oldObject, isOnLoadCreate, DoneAction, isGridInvoker, gridRow, true);
                                    if (_ruleExecutionResult.RULE_ACTION_STATUS != 6
                                            && _ruleExecutionResult.RULE_ACTION_STATUS != 7) {
                                        STAGESystemOut.PrintNonDebugError("Record not saved. Rule # " + rule.getRecId()
                                                + " is invalid Rule Description (" + rule.getUserMessage() + ")");
                                        String ruleMessage = (Utils.isNotEmpty(object.getRuleMessage())) ? object.getRuleMessage() : rule.getUserMessage();
                                        throw new NTGException(ruleMessage);
                                    } else {
                                        break;
                                    }
                                case WEB_SERVICE_CALL:
                                    actionWebService.fireWebService(rule.getIntegrationRepo().getRequest(), object,
                                            objectRecId, rule.getIntegrationRepo().getInputs(), rule.getUserInputs(),
                                            rule.getIntegrationRepo().getServiceURL(),
                                            rule.getIntegrationRepo().getSoapAction(), rule,
                                            oldObject, rule.getIntegrationRepo().getHttpMethod(), ruleType, isGridInvoker, gridRow, rule.getUserHeaderInputs());
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case SLA:
                                    slaEngine.applySLA(object, rule.getSlaAction(), rule.getSlaSelected(),
                                            rule.getIsAllProfiles());
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case FORM_MAPPING:
                                    // From Mapping
                                    dbMapping.databaseSearch(object, objectRecId, rule, ruleType,
                                            dbMappingRulesMap, oldObject, isOnLoadCreate, isGridInvoker, gridRow);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case DB_INVOKER:
                                    // Remote DB Invoker
                                    remoteDBMapping.remoteDataBaseSearch(object, objectRecId, rule,
                                            oldObject, dbMappingRulesMap, ruleType, isOnLoadCreate, isGridInvoker, gridRow);

                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case PUSH_TEMPLATE_DB:
                                    // PUSH Template based on form Mapping
                                    actionPushTemplate.templatePush(object, rule,
                                            dbMappingRulesMap, oldObject, isGridInvoker, gridRow);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case STATIC_MAPPING:
                                    // STATIC Mapping
                                    staticMapping.doStaticMapping(object, rule, staticMappingRulesMap, oldObject, isGridInvoker, gridRow,DoneAction, gridRowIndex);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case PUSH_TEMPLATE:
                                    actionPushTemplate.templatePush(object, rule, dbMappingRulesMap, oldObject, isGridInvoker, gridRow);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case PUSH_DATA_TO_OTHER_OBJECT:
                                    pushingDataIntoObject.selectFromTableAndInsertInAnotherTable(object, currentRule, rule, oldObject, DoneAction, isGridInvoker, gridRow);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case NO_ACTION:
                                    // NO Action don't use it (is needed)
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case EXCUTE_SQL:
                                    // PL SQL call
                                    actionExecuteSQL.executeSQL(object, rule, oldObject, isGridInvoker, gridRow,DoneAction);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case EXIT_ACTION:
                                    // Exit Action
                                    if (rule.getExitAction() == 1) {
                                        exitActionVar = rule.getParentId();
                                    } else if (rule.getExitAction() == 2) {
                                        _ruleExecutionResult.exitFromAllRules = -1;
                                    } else {
                                        throw new NTGException("", "Rule " + rule.getRecId() + " Rule Type Id = "
                                                + rule.getTypeId() + "Rule Object Id =" + objectRecId);
                                    }
                                    break;

                                case COPY_PLAN_TEMPLATE:
                                    // Copy Template call
                                    copyPlanTemplate.copyPlanTemplate(object, rule);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case MESSAGE_QUEUE:
                                    // Message Queue
                                    messageQueue.ScheduleMessage(object, rule);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;

                                case JS_RULE:
                                    actionJsRule.jsRuleExecute(object, rule);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                case RELOAD_CHARTS:
                                    startReloadCharts(object, rule);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                  //  by ahmed eldeeb Dev-00003200
                                case ShortCut_Navigation:
                                    object.setShortcutRule("Shortcut Rule");
                                    object.setShortCutId(rule.getUdaIdRuleInvoker());
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                //  by ahmed eldeeb Dev-00003094
                                case EXPORT_ACTION:
                                    object.setSelectedVal(rule.getSelectedVal());
                                    object.setUdaGridType(rule.getUdaGridType());
                                    object.setExportType(rule.getExportType());
                                    object.setExportMessage("Export Action Rule");
                                    Map <String,String > hm = new HashMap<String,String >();
                                    hm.put("selectedVal",rule.getSelectedVal());
                                    hm.put("udaGridType",rule.getUdaGridType());
                                    hm.put("exportType",rule.getExportType());
                                    myList.add(hm);
                                    object.setRuleObject(myList);
                                    object.setExportRuleId(rule.getUdaIdRuleInvoker());
                                    object.setSelectedValnew(rule.getSelectedValnew());
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                    
                                case Plugin:
                                    startPlugin(object, rule, staticMappingRulesMap, oldObject, isGridInvoker, gridRow,DoneAction);
                                    RuleLogger.logRule(rule, currentRule, true, objectRecId);
                                    break;
                                default:
                                    System.out.println(rule.getUserMessage());
                                    break;

                            }


                        } else {
                            // rule is not trueRuleLogger.logRule(rule, currentRule, false, objectRecId);
                            ignored.add(rule.getRecId());
                        }

                    } catch (NTGException ng) {

                        if (_ruleExecutionResult.RULE_ACTION_STATUS == 0) {
                            throw new Exception(rule.getExitActionException());
                        } else if (_ruleExecutionResult.RULE_ACTION_STATUS == 3
                                || _ruleExecutionResult.RULE_ACTION_STATUS == 5
                                || _ruleExecutionResult.RULE_ACTION_STATUS == 1) {
                            throw new PreventSaveException("", ng.getMessage());
                        } else {
                            STAGESystemOut.PrintNonDebugError(
                                    "Record is not saved. Rule is Fail# " + rule.getRecId() + " Rule Type Id = "
                                            + rule.getTypeId() + "Rule Object Id =" + objectRecId);
                            throw new PreventSaveException("", "Rule " + rule.getRecId() + " Rule Type Id = "
                                    + rule.getTypeId() + "Rule Object Id =" + objectRecId);
                        }
                    } catch (Exception e) {
                        if (rule.getRULE_ACTION() == 14) {
                            _ruleExecutionResult.RULE_ACTION_STATUS = 3;
                        }
                        // rule is true but the action is fail
                        NTGMessageOperation.PrintErrorTrace(e);
                        if (ruleType.equals("before")) {

                            STAGESystemOut.PrintNonDebugError("Ticket is not saved. Rule is Fail# "
                                    + rule.getRecId() + " Rule Type Id = " + rule.getTypeId());
                            //throw new PreventSaveException("", "Rule " + rule.getRecId() + " Rule Type Id = "
                                   // + rule.getTypeId() + "Rule Object Id =" + objectRecId);

                        }

                        ruleLogger.actionFail(object, rule, objectRecId);
                        //samaaEissa Dev-00003079:Issues in Rule Invoker with (Call Rest Web Service) Rule
                        String message=e.getMessage();
                        int start=e.getMessage().indexOf("message");
                        int end=e.getMessage().indexOf("locale");
            if(start>-1 && end>-1){
                message= message.substring(start+9,end-2);
            }
                                    throw new NTGException("0000", message);
                    }
                    if (exitActionVar != 0L)
                        continue;
                }
            } else {
                // rule is disabled
                ignored.add(rule.getRecId());
            }
        } // end for

    }

    /**
     * @param object generic object
     * @param rule   Current RULE
     * @Description set reload chart udas with the object
     */
    private void startReloadCharts(GenericObject object, RuleBuilder rule) {
        object.setReloadCharts(true);
        if (Utils.isEmpty(object.getReloadChartsUdas())) {
            object.setReloadChartsUdas(new ArrayList<>());
        }
        object.getReloadChartsUdas().add(rule.getReloadCharts());
    }

    /**
     * @param rule Current RULE
     * @return Rule after replacing the Logical operations
     */

    public String biuldActualRule(String rule) {

        if (rule.contains("AND")) {
            rule = rule.replace("AND", "&&");
        }

        if (rule.contains("OR")) {
            rule = rule.replace("OR", "||");
        }

        if (rule.contains("AND") || rule.contains("OR")) {
            biuldActualRule(rule);
        } else {
            return rule;
        }

        return rule;
    }

    /**
     * @param userQuery
     * @return
     */

    public String buildActualQuery(String userQuery , Boolean isFormMaping) {

        boolean staticVal = true ;
        if (userQuery == null)
            return null;

        if (userQuery.contains("$")) {
            userQuery = userQuery.replace("$", "");
            staticVal =false;
        }

        if (userQuery.contains("uda.")) {
            userQuery = userQuery.substring(4);
            staticVal =false;

        }

        if (userQuery.contains("Object.")) {
            userQuery = userQuery.replace("Object.", "");
            staticVal =false;

        }
        if (userQuery.contains("$")) {
            buildActualQuery(userQuery, isFormMaping);
            staticVal =false;

        }
        else {
             if(isFormMaping && staticVal)
                return "THIS_IS_STATIC_VALUE ," +  userQuery;
            else
                return userQuery;
        }

        return userQuery;
    }

    /**
     * @param currentRule
     * @param objectRecId
     * @param ruleType    (before saving , after saving)
     * @return
     */

    private boolean isValidBackendRule(RuleBuilder currentRule, long objectRecId, String ruleType, boolean isNew) {

        if (ruleType.equals("invoker") || ruleType.equals("Engine"))
            return true;
//addedBy:Aya.Ramadan to fix loading in creation issue 
        if (ruleType.equals("before") && isNew && currentRule.isLoading()) {
            return true;
        }
        if (ruleType.equals("onLoad"))
            return true;
        if (ruleType.equals("before") && !isNew && currentRule.getOnLoad()) {
            return true;
        }

        if (ruleType.equals("before") && !isNew && currentRule.isModification()) {
            return true;
        }
        if (ruleType.equals("before") && isNew && currentRule.isCreation()) {
            return true;
        }
        if (ruleType.equals("after") && isNew && currentRule.isAfterCreation()) {
            return true;
        }
        if (ruleType.equals("after") && !isNew && currentRule.isAfterModification()) {
            return true;
        }

        return false;
    }


    /**
     * Function to display prevent save message from rule execution to user
     *
     * @param object
     * @param userMessage
     * @param rule
     * @param ruleType
     * @param oldObject
     * @param isOnLoadCreate
     * @throws Exception
     */
    public void preventObjectSave(Object object, String userMessage, RuleBuilder rule, String ruleType,
                                  Object oldObject, boolean isOnLoadCreate, String DoneAction, boolean isGridInvoker, Map<String, Object> gridRow, boolean isConditionCheck) throws Exception {

        if (userMessage != null) {
            userMessage = typesUdaService.replaceObjectsNamesWithITSValues(userMessage, object, true, 0, 0, oldObject, DoneAction, isGridInvoker, gridRow, isConditionCheck);
        }
        if (_ruleExecutionResult.RULE_ACTION_STATUS == 5) {
            object.getClass().getMethod("setPreventSave", boolean.class).invoke(object, true);
            if (ruleType.equals("before"))
                throw new NTGException(userMessage);

        } else {
            _ruleExecutionResult.RULE_ACTION_STATUS = 1;
            object.getClass().getMethod("setPreventSave", boolean.class).invoke(object, true);

            object.getClass().getMethod("setRuleMessage", String.class).invoke(object, "");
            object.getClass().getMethod("setRuleMessage", String.class).invoke(object, userMessage);

            if (ruleType.equals("before") && !isOnLoadCreate)
                throw new NTGException(userMessage);
            if (ruleType.equals("after") && !isOnLoadCreate)
                throw new NTGException(userMessage);
            if (ruleType.equals("onLoad") || isOnLoadCreate) {
                _ruleExecutionResult.RULE_ACTION_STATUS = 6;
                object.getClass().getDeclaredMethod("setNotificationFlag", Boolean.class).invoke(object, true);
                object.getClass().getMethod("setNotificationMessagesIntoArray", String.class).invoke(object,
                        userMessage);
            }
        }

    }

    public void clearPreventSaveValues(Object object) throws Exception {

        _ruleExecutionResult.RULE_ACTION_STATUS = 0;
        object.getClass().getMethod("setPreventSave", boolean.class).invoke(object, false);

    }
    //@samaa Eissa Dev-00002616:Rules --> rules doesn't execute  with sequence for parents and child
    public List<RuleBuilder>sortRulesOrder(List<RuleBuilder> rules){
        List<RuleBuilder> sortedRules=new ArrayList<>();
        CreateTree(rules,Long.valueOf(-1),sortedRules);
        return sortedRules;
    }
    //@samaa Eissa Dev-00002616:Rules --> rules doesn't execute  with sequence for parents and child
    public void CreateTree(List<RuleBuilder> rules,Long parentId,List<RuleBuilder> sortedRules)
    {
      for(RuleBuilder rule : rules)
      {
          Long ruleParentID=rule.getParentId();
          if(ruleParentID==null || ruleParentID==-1)  {
              ruleParentID = Long.valueOf(-1);
          }
          if(ruleParentID.equals(parentId))
          {
              sortedRules.add(rule);
              Long ruleID=rule.getRecId();
              CreateTree(rules,ruleID,sortedRules);
          }
      }
    }

    /**
     * @param object 
     * @param rule 
     * @param object generic object
     * @param rule   Current RULE
     * @param doneAction 
     * @param gridRow 
     * @param isGridInvoker 
     * @param oldObject 
     * @param staticMappingRulesMap 
     * @throws Exception 
     * @Description set reload chart udas with the object
     */
	private void startPlugin(GenericObject object, RuleBuilder rule, Map<Long, RuleStaticMapping> staticMappingRulesMap, Object oldObject, boolean isGridInvoker, Map<String, Object> gridRow, String doneAction) throws Exception {
    	// load plugin repo
		Optional<com.ntg.Smart2Go.internal.entites.Plugin> plugin = PluginRepo.findById(rule.getPluginId());
		
		// change pluginURL
		changePluginURL(plugin.get().getPluginUrl());
		
		// retrieves the spring application context
    			ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);

    			HashMap<String, Object> map = new HashMap<>();
    			
    			
//    			List<String[]> listOfImpacts = new ArrayList<String[]>();
    			List<String[]> listOfImpactsValues = new ArrayList<String[]>();
//    			RuleStaticMapping ruleStaticMapping = staticMappingRulesMap.get(rule.getStaticMappingId());

    			// check for null pointer
    			if (Utils.isNotEmpty(rule.getPluginKeys()) && Utils.isNotEmpty(rule.getPluginValues())) {
//    				listOfImpacts = rule.getPluginKeys();
    				listOfImpactsValues = rule.getPluginValues();
    			}

    			List<UDAsWithValues> UdaValues = (List<UDAsWithValues>) object.getClass().getMethod("getUdasValues", null)
    					.invoke(object, null);

    			Map<String, UDAsWithValues> udsMap = null;

    			int currentRuleIndex = 0;
    			int flagPush = 0;

//    					String[] currentImpacts = listOfImpacts.get(currentRuleIndex);
    					String[] currentImpactsValues = listOfImpactsValues.get(currentRuleIndex);
    					Map<String, List<HashMap<String, Object>>> gridMap = new HashMap<>();

    					for (int i = 0; i < currentImpactsValues.length; i++) {
    						currentImpactsValues[i] = currentImpactsValues[i].replace("{{", "").replace("}}", "");
    						String currentImpactsName = buildActualQuery(currentImpactsValues[i],false);

    						if (Utils.isNotEmpty(currentImpactsValues[i])) {
    							currentImpactsValues[i] = currentImpactsValues[i].replaceAll(" ", "[rsp]");
    						}
    						String currentImpactValue = "";
    						if(!currentImpactsValues[i].contains("_att_val")) {
    					
    						 currentImpactValue = typesUdaService
    								.replaceObjectsNamesWithITSValues(currentImpactsValues[i], object, false, 0, 0, oldObject, null ,isGridInvoker, gridRow, false);
    						} 
    						// replace date identifier and spaces after replace value 
    						currentImpactValue = currentImpactValue.replaceAll("\\[rsp]", " ").replaceAll("DATE_", "");
    						if (currentImpactsValues[i].contains("uda")) {

    							// convert UDAs to map to fast searchs
    							if (udsMap == null) {
    								udsMap = staticMapping.getUDAMapByUDAName(UdaValues);
    							}
    							String[] files; 
    							// find the uda
    							String[] actualImpactName = currentImpactsName.split("\\.");
    							UDAsWithValues Theuda = udsMap.get((actualImpactName[0].endsWith("_att_val")) ? actualImpactName[0].replace("_att_val", "") : actualImpactName[0]);
    							if (Theuda != null) {

    								switch ((int) Theuda.getUdaType()) {
    								case IUDAType.NUMERIC:
    								case IUDAType.TEXT:
    									currentImpactValue=currentImpactValue.replace("'","");
    									currentImpactValue = Utils.removeHtmlTags(currentImpactValue);
    								case IUDAType.MEMO:
    									currentImpactValue = Utils.removeHtmlTags(currentImpactValue);
    								case IUDAType.DATE:
    									Theuda.setUdaValue((currentImpactValue.equals("null") || Utils.isEmptyString(currentImpactValue)) ? null : currentImpactValue);
    									Date date = staticMapping.formatDateBasedOnUDA(Theuda, currentImpactValue);
    									
    									// set date as long for consider time zone in front end
    									if (date != null) {
    										Theuda.setUdaDateAsLong(date.getTime());
    									}
    									RuleBuilderController.udaMappedValues = UdaValues;
    									object.getClass().getMethod("setUdasValues", List.class).invoke(object, UdaValues);
    									map.put("inp", currentImpactValue);
    									break;
    								case IUDAType.VALUE_LIST:

    									UdaMultiValue selectedObj = new UdaMultiValue();
    									selectedObj.setName((currentImpactValue.equals("null") ? null : currentImpactValue));
    									Theuda.setSelectedObj(selectedObj);
    									Theuda.setUdaValue(selectedObj.getName());
    									map.put("inp", currentImpactValue);
    									break;
    								case IUDAType.FORM:
    									
    									if (!Utils.isEmptyString(currentImpactValue) && !currentImpactValue.equals("null")) {
    										if (NumberUtils.isDigits(currentImpactValue)) {
    											TypesUDa uda = new TypesUDa();
    											
    											uda.setFormTypeId(Theuda.getFormTypeId());
    											uda.setRecId(Theuda.getRecId());
    											uda.setFormUdaSelectedFields(Theuda.getFormUdaSelectedFields());
    											uda.setFormUdaOrderBy(Theuda.getFormUdaOrderBy());
    								            uda.setFormGridId(Theuda.getFormGridId());
    								            
    											Theuda.setUdaValue(currentImpactValue);
    											Theuda.setUdaFormValueString(
    													typesUdaService.getFormListValue(Long.valueOf(currentImpactValue), uda, null));
    										}

    									}
    									break;
    								case IUDAType.HTEXT:
    									Theuda.setUdaValue((currentImpactValue.equals("null") || Utils.isEmptyString(currentImpactValue)) ? null : currentImpactValue);
    									break;
    								case IUDAType.CHECK_BOX:
    									if (currentImpactValue.equals("null") || currentImpactValue.equals("false")
    											|| currentImpactValue.equals("0"))
    										Theuda.setUdaValue("false");
    									else
    										Theuda.setUdaValue("true");
    									break;
    								case IUDAType.ASSIGNMENT_INFO:
    									UDAsWithValues assignmentUda = Theuda;

    									char c = Character.toUpperCase(actualImpactName[1].charAt(0));
    									String setterMethod = "set" + c + actualImpactName[1].substring(1);

    									assignmentUda.getClass().getDeclaredMethod(setterMethod, String.class)
    											.invoke(assignmentUda, currentImpactValue);
    									break;

    								case IUDAType.ATTACHMENT:
    									files = new String [Theuda.getAttachemnts().size()];
    									int index = 0;
   									for ( Attachment attach : Theuda.getAttachemnts()) {
    										files[index] = System.getProperty("user.home") + File.separator + ".Smart2GoConfig"
    												+ File.separator + attach.getAttachmentUrl();
    										index++;
   									}
									map.put("inp", files);
//    								
    									break;
    									
    								case IUDAType.GRID:
    									// handle grid mapping
    									String impactedColumn = currentImpactsName.split("\\.")[1];
    									List<HashMap<String, Object>> listRow = gridMap.get(actualImpactName[0]);
    									if (Utils.isNotEmpty(listRow)) {
    										if (!Utils.isEmptyString(currentImpactValue)) {
    											String[] arrValue = currentImpactValue.replaceAll("\\[", "").replaceAll("\\]", "").split(",");
    											if (Utils.isNotEmpty(arrValue)) {
    												for (int x = 0; x < arrValue.length && x < listRow.size(); x++) {
    													listRow.get(x).put(impactedColumn.toLowerCase(), arrValue[x]);
    												}

    												gridMap.put(actualImpactName[0], listRow);
    											}
    										}

    									}

    									else {
    										listRow = new ArrayList<HashMap<String, Object>>();
    										if (!Utils.isEmptyString(currentImpactValue)) {
    											HashMap<String, Object> row = null;

    											String[] arrValue = currentImpactValue.replaceAll("\\[", "").replaceAll("\\]", "").split(",");

    											if (Utils.isNotEmpty(arrValue)) {
    												for (String val : arrValue) {
    													row = new HashMap<String, Object>();
    													row.put(impactedColumn.toLowerCase(), val);
    													listRow.add(row);
    												}
    											}

    											gridMap.put(actualImpactName[0], listRow);
    										}

    									}
    									break;
    								}
    							}

    						} 
        					currentImpactsValues[i] = currentImpactValue;

    					}
//    			int j = 0;
//    		      for(String key : currentImpacts) {
//    		    	  if(currentImpactsValues[j]!=null) map.put("inp", currentImpactsValues[j]);
//    		    	  j++;
//    		      }


      			// retrieves automatically the extensions for the GenericPluginInterfaceImp.class extension point
    			GenericPluginInterfaceImp genericPluginInterface = applicationContext.getBean(GenericPluginInterfaceImp.class);
    			HashMap<String, Object> result = genericPluginInterface.callPlugin(map);
    			int k = 0;
//    			for(String[] key : rule.getPluginOutputKeys()) {
//  		    	  
//  		    	 if(result!=null) {
//  		    		 Object out = result.get(key[k]);
//  	    			for(UDAsWithValues uda : UdaValues) {
//  	    				String resolvedUdaName =buildActualQuery(rule.getPluginOutputValues().get(0)[k].replace("}}", "").replace("{{", ""),false);
//  	    				if(uda.getUdaName().equals(resolvedUdaName)) {
//  	    					switch ((int) uda.getUdaType()) {
//							case IUDAType.TEXT:
//							case IUDAType.NUMERIC:
//							case IUDAType.CHECK_BOX:
//							case IUDAType.VALUE_LIST:
////								uda.setUdaValue(out);
//								break;
//							case IUDAType.GRID:
////								uda.setUdaValue(out);
//								break;
//							default:
//								break;
//							}
//  	    					
//  	    				}
//  	    				}
//  		    		 }
//  		    		  
//  		    	 k++;
//  		      }
		System.out.println(result);
    			// stop plugins
//    			PluginManager pluginManager = applicationContext.getBean(PluginManager.class);

//    	    	        pluginManager.stopPlugins();
//    	    	 // retrieves the spring application context
//    	        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringConfiguration.class);
    	//
//    	        // retrieves automatically the extensions for the Greeting.class extension point
//    	        Greetings greetings = applicationContext.getBean(Greetings.class);
//    	        greetings.printGreetings();
    }
   
	public void changePluginURL(String pluginPath) throws Exception {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			String path = System.getProperty("user.home") + "/.Smart2GoConfig/.config.properties"; 
			input = new FileInputStream(path);
			File file = new File(path);
			// load a properties file
			prop.load(input);
			// update plugin_path property
			prop.get("plugin_path");
			prop.setProperty("plugin_path",pluginPath);	
			Application.StoreTheNewPropertiyFile(prop, "config.properties",file);
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					NTGMessageOperation.PrintErrorTrace(e);
				}
			}
		}
	}



}
