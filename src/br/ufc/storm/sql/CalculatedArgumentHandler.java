package br.ufc.storm.sql;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

import com.udojava.evalex.Expression;

import br.ufc.storm.control.Resolution;
import br.ufc.storm.exception.DBHandlerException;
import br.ufc.storm.exception.FunctionException;
import br.ufc.storm.io.LogHandler;
import br.ufc.storm.jaxb.CalculatedArgumentType;
import br.ufc.storm.jaxb.CalculatedFunctionTermType;
import br.ufc.storm.jaxb.CalculatedFunctionType;
import br.ufc.storm.jaxb.CalculatedParameterType;
import br.ufc.storm.jaxb.CandidateListType;
import br.ufc.storm.jaxb.ContextArgumentType;
import br.ufc.storm.jaxb.ContextContract;
import br.ufc.storm.jaxb.ContextParameterType;
import br.ufc.storm.mcdm.DecisionMatrix;
import br.ufc.storm.model.ArgumentTable;
import br.ufc.storm.model.ResolutionNode;

public class CalculatedArgumentHandler extends DBHandler{
	private static final String INSERT_CALCULATED_FUNCTION = "INSERT INTO calculated_function (cc_id, function_value, cp_id, CF_TYPE) VALUES (?,?,?,?) RETURNING cf_id;";
	private static final String INSERT_CALCULATED_PARAMETER = "INSERT INTO calculated_function_terms(cf_id,term_order,cp_id) VALUES (?,?,?);";
	private static final String SELECT_CALCULATED_FUNCTION = "select * FROM calculated_function A, context_parameter B WHERE B.cp_id = ? AND A.cc_id = ? AND B.parameter_type = ? AND A.cp_id = B.cp_id ORDER BY B.cp_id;";
	private static final String SELECT_CALCULATED_FUNCTION_GENERIC = "select * FROM calculated_function A, context_parameter B WHERE B.cp_id = ? AND A.cc_id IS NULL AND B.parameter_type = ? AND A.cp_id = B.cp_id ORDER BY B.cp_id;";
	private static final String SELECT_CALCULATED_FUNCTION_TERMS = "SELECT * FROM calculated_function_terms WHERE cf_id = ? ORDER BY term_order;";
	private static final String SELECT_CALCULATED_PARAMETER = "select * FROM context_parameter WHERE ac_id = ? AND parameter_type = ? ORDER BY cp_id;";
	private static final String SELECT_CFT_ID_CC_ID_GENERIC = "SELECT * FROM calculated_function WHERE cp_id = ?;"; 
	private static final String SELECT_CFT_ID_CC_ID_SPECIFIC = "SELECT * FROM calculated_function WHERE cp_id = ? AND cc_id = ?;"; 

	public static ArrayList<DecisionMatrix> decisionMatrix = new ArrayList<>();

	public static void main(String[] args) {
		//try {
		//			QualityFunctionType cft = getQualityFunction(1, 123);
		//			System.out.println(cft.getFunctionValue());

		/*CalculatedFunctionType q = new CalculatedFunctionType();
			q.setCpId(2);
			q.setCcId(123);
			q.setFunctionValue("V0-v1");


			CalculatedFunctionTermType qft = new CalculatedFunctionTermType();
			qft.setCpId(27);
			qft.setOrder(1);
			q.getFunctionParameters().add(qft);
			CalculatedFunctionTermType qft2 = new CalculatedFunctionTermType();
			qft2.setCpId(27);
			qft2.setOrder(2);
			q.getFunctionParameters().add(qft2);
			addCalculatedFunction(q, 1);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
		try {
			Resolution.main(args);
		} catch (DBHandlerException e) {
			e.printStackTrace();
		}
	}


	public static boolean exists(CalculatedFunctionType cft) throws DBHandlerException{
		if(cft==null){
			throw new DBHandlerException("Context argumente null");
		}else{
			if(cft.getCpId() == null || cft.getFunctionValue()==null){
				throw new DBHandlerException("Calculated function incomplete");
			}
		}
		try {
			Connection con = getConnection(); 
			PreparedStatement prepared;
			if(cft.getCcId()==null){
				prepared = con.prepareStatement(SELECT_CFT_ID_CC_ID_GENERIC);
				prepared.setInt(1, cft.getCpId());
			}else{
				prepared = con.prepareStatement(SELECT_CFT_ID_CC_ID_SPECIFIC);
				prepared.setInt(1, cft.getCpId());
				prepared.setInt(2, cft.getCcId());
			}
			ResultSet resultSet = prepared.executeQuery();
			int cont = 0;
			int id = 0;
			while(resultSet.next()){
				id = resultSet.getInt("cf_id");
				cont++;
			}
			if(cont==0){
				return false;
			}else{
				if(cont == 1){
					cft.setFunctionId(id);
					return true;
				}else{
					throw new DBHandlerException("Multiple context arguments where found: ");
				}
			}
		} catch (SQLException e) {
			throw new DBHandlerException("A sql error occurred: ", e);
		} 
	}

	/**
	 * 
	 * @return
	 * @throws DBHandlerException 
	 */
	public static Integer addCalculatedFunction(CalculatedFunctionType qf, int type) throws DBHandlerException{
		if(exists(qf)){
			return qf.getFunctionId();
		}
		int function_id = 0;
		try {
			Connection con = getConnection(); 
			con.setAutoCommit(false);
			PreparedStatement prepared = con.prepareStatement(INSERT_CALCULATED_FUNCTION); 
			prepared.setInt(1, qf.getCcId());
			prepared.setString(2, qf.getFunctionValue());
			prepared.setInt(3, qf.getCpId());
			prepared.setInt(4, type);
			ResultSet resultSet = prepared.executeQuery();
			if(resultSet.next()){
				function_id = resultSet.getInt("cf_id");
				for(CalculatedFunctionTermType cp: qf.getFunctionParameters()){
					prepared = con.prepareStatement(INSERT_CALCULATED_PARAMETER); 
					prepared.setInt(1, function_id); 
					prepared.setInt(2, cp.getOrder()); 
					prepared.setInt(3, cp.getCpId());
					prepared.execute();
				}
				con.commit();
				return function_id;	
			}else{
				throw new DBHandlerException("Quality function can not be created, error trying retrieve qf id");
			}
		} catch (SQLException e) { 
			throw new DBHandlerException("A sql error occurred: ", e);
		} 

	}


	/**
	 * 
	 * @param ac_id
	 * @param connection
	 * @return
	 * @throws DBHandlerException
	 */

	public static ArrayList<CalculatedParameterType> getCalculatedParameters(int ac_id, int type) throws DBHandlerException{

		try{	
			ArrayList<CalculatedParameterType> cps= new ArrayList<CalculatedParameterType>();
			Connection con = getConnection();
			PreparedStatement prepared = con.prepareStatement(SELECT_CALCULATED_PARAMETER);
			prepared.setInt(1, ac_id);
			prepared.setInt(2, type);
			ResultSet resultSet = prepared.executeQuery(); 
			while(resultSet.next()){ 
				CalculatedParameterType calcp = new CalculatedParameterType();
				calcp.setCalcId(resultSet.getInt("cp_id"));
				calcp.setName(resultSet.getString("cp_name"));
				calcp.setKindId(resultSet.getInt("variance_id"));
				calcp.setCpUnit(resultSet.getString("cp_unit"));
				cps.add(calcp);
			}
			return cps;
		} catch (SQLException e) { 
			throw new DBHandlerException("A sql error occurred: ", e);
		} 	
	}

	/**
	 * 
	 * @return
	 * @throws DBHandlerException 
	 */
	public static CalculatedFunctionType getCalculatedFunction(int cp_id, int cc_id, int type) throws DBHandlerException{
		try { 
			Connection con = getConnection(); 
			PreparedStatement prepared = con.prepareStatement(SELECT_CALCULATED_FUNCTION);
			prepared.setInt(1, cp_id);
			prepared.setInt(2, cc_id);
			prepared.setInt(3, type);
			ResultSet resultSet = prepared.executeQuery();
			CalculatedFunctionType qf = null;
			if(resultSet.next()) {
				qf = new CalculatedFunctionType();
				qf.setFunctionId(resultSet.getInt("cf_id"));
				qf.setFunctionValue(resultSet.getString("function_value"));
				return qf;
			}else{
				prepared = con.prepareStatement(SELECT_CALCULATED_FUNCTION_GENERIC);
				prepared.setInt(1, cp_id);
				prepared.setInt(2, type);
				resultSet = prepared.executeQuery();
				if(resultSet.next()) {
					qf = new CalculatedFunctionType();
					qf.setFunctionId(resultSet.getInt("cf_id"));
					qf.setFunctionValue(resultSet.getString("function_value"));
					return qf;
				}
			}
			return qf;
		} catch (SQLException e) { 
			e.printStackTrace();
			throw new DBHandlerException("A sql error occurred: ", e);
		}		

	}
	/**
	 * 
	 * @param cf_id
	 * @return
	 * @throws DBHandlerException
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<CalculatedFunctionTermType> getCalculatedFunctionParameter(int cf_id) throws DBHandlerException{
		ArrayList<CalculatedFunctionTermType> cps= new ArrayList<CalculatedFunctionTermType>();
		try { 
			Connection con = getConnection(); 
			PreparedStatement prepared = con.prepareStatement(SELECT_CALCULATED_FUNCTION_TERMS); 
			prepared.setInt(1, cf_id);
			ResultSet resultSet = prepared.executeQuery(); 
			while(resultSet.next()){ 
				CalculatedFunctionTermType cftt = new CalculatedFunctionTermType();
				cftt.setCpId(resultSet.getInt("cp_id"));
				cftt.setOrder(resultSet.getInt("term_order"));
				cftt.setWeight(resultSet.getFloat("cft_weight"));
				cps.add(cftt);
			}
			Collections.sort (cps, new ComparatorCalculatedTerms(true));
			return cps;
		} catch (SQLException e) { 
			throw new DBHandlerException("A sql error occurred: ", e);
		}	
	}

	public static Object getContractArgument(int cp_id, ContextContract cc){
		for(ContextArgumentType cat : cc.getContextArguments()){
			if(cat.getContextParameter().getCpId()==cp_id){
				return cat.getValue();
			}else{
				if(cat.getContextContract()!=null){
					return getContractArgument(cp_id, cat.getContextContract());
				}else{
					//variavel compartilhada
				}
			}
		}
		return null;
	}

	public static int calulateContextContractArguments(ContextContract cc, ArgumentTable argTable, int type) throws FunctionException{
		//Calculates arguments contracts parameters before it own quality arguments

		int count = 0;
		for(ContextArgumentType cat : cc.getContextArguments()){
			//pegar o parametro
			Integer kind=-1;


			for(ContextParameterType cpt:ResolutionNode.resolutionTree.findNode(cc.getAbstractComponent().getIdAc()).getCps()){
				if(cpt.getCpId()==cat.getContextParameter().getCpId()){
					kind = cpt.getKind();
				}
			}
			if(kind == ContextParameterHandler.CONTEXT && cat.getContextContract()!=null){
				CalculatedArgumentHandler.calulateContextContractArguments(cat.getContextContract(), argTable, type);
			}
		}
		List<CalculatedParameterType> calcps = null;
		switch (type) {
		case ContextParameterHandler.QUALITY:
			calcps= ResolutionNode.resolutionTree.findNode(cc.getAbstractComponent().getIdAc()).getQps();
			break;
		case ContextParameterHandler.COST:
			calcps= ResolutionNode.resolutionTree.findNode(cc.getAbstractComponent().getIdAc()).getCops();
			break;
		case ContextParameterHandler.CALCULATEDCONTEXT:
			calcps= ResolutionNode.resolutionTree.findNode(cc.getAbstractComponent().getIdAc()).getCaps();
			break;
		case ContextParameterHandler.RANKING:
			calcps= ResolutionNode.resolutionTree.findNode(cc.getAbstractComponent().getIdAc()).getRps();
			break;
		}

		if(cc.getPlatform()!=null){
			count+=calulateContextContractArguments(cc.getPlatform().getPlatformContract(), argTable, type);
		}
		//Begin of calculus
		for(CalculatedParameterType calcpt:calcps){//calcula cada parametro

			CalculatedFunctionType function = null;
			try {
				function = CalculatedArgumentHandler.getCalculatedFunction(calcpt.getCalcId(), cc.getCcId(), type);
				if(function != null){
					ArrayList<CalculatedFunctionTermType> terms = null;
					terms = CalculatedArgumentHandler.getCalculatedFunctionParameter(function.getFunctionId());
					for(CalculatedFunctionTermType qftt: terms){//busca cada argumento de cada termo da função
						ContextArgumentType cat = argTable.getArgument(qftt.getCpId());
						if(cat!=null){
							function.getFunctionArguments().add(cat);
						}
					}
					CalculatedArgumentType qat = new CalculatedArgumentType();
					qat.setCpId(calcpt.getCalcId());
					function.setCpId(qat.getCpId());
					function.setCcId(cc.getCcId());
					qat.setValue(CalculatedArgumentHandler.calculate(function, null));
					if(qat.getValue()!=null){
						argTable.addNewArgument(calcpt.getCalcId(), ""+qat.getValue(), calcpt.getKindId());
						calcpt.setCalculatedArgument(qat);

						switch (type) {
						case ContextParameterHandler.QUALITY:
							cc.getQualityArguments().add(qat);
							count++;
							break;
						case ContextParameterHandler.COST:
							cc.getCostArguments().add(qat);
							count++;
							break;
						}
					}

				}
			} catch (Exception e) {
				LogHandler.getLogger().info("Error while getting function. "+e.getCause());
				e.printStackTrace();
			}
		}

		return count;
	}


	public static int calulateRankArguments(CandidateListType cl, Hashtable <Integer , Hashtable <Integer , ArgumentTable>> tableOfSWidArgumentTable) throws FunctionException{
		int count = 0;
		int rankParametersCount = 0;


		for(int c = 0; c < cl.getCandidate().size(); c++){
			ContextContract cc = cl.getCandidate().get(c);
			ArgumentTable argTable = tableOfSWidArgumentTable.get(cc.getCcId()).get(cc.getPlatform().getPlatformContract().getCcId());
			List<CalculatedParameterType> calcps = ResolutionNode.resolutionTree.findNode(cc.getAbstractComponent().getIdAc()).getRps();
			if(cl.getCandidate().size() > 0 && decisionMatrix.size() == 0){
				rankParametersCount = calcps.size();
				//testar esse metodo e carregar pesos e domínios do banco
				int numOfMatrix = calcps.size();
				for(int i = 0; i < numOfMatrix; i++){
					DecisionMatrix d = new DecisionMatrix(calcps.get(i).getName());
					decisionMatrix.add(d);
				}
			}
			CalculatedParameterType calcpt;
			for(int i = 0; i < calcps.size(); i++){//calcula cada parametro
				calcpt = calcps.get(i);
				CalculatedFunctionType function = null;
				try {
					function = CalculatedArgumentHandler.getCalculatedFunction(calcpt.getCalcId(), cc.getCcId(), ContextParameterHandler.RANKING);
					ArrayList<CalculatedFunctionTermType> terms = CalculatedArgumentHandler.getCalculatedFunctionParameter(function.getFunctionId());
					if(function != null){
						if(decisionMatrix.get(i).getMatrix()==null){
							if(terms.size() > 0){
								for(int j = 0; j < terms.size(); j++){
									ContextParameterType cpt = ContextParameterHandler.getContextParameter(terms.get(j).getCpId());
									decisionMatrix.get(i).addCriterion(cpt.getKind(), terms.get(j).getWeight(), terms.get(j).getCpId(), cpt.getName());
								}
								decisionMatrix.get(i).createMatrix(cl.getCandidate().size(), terms.size());
							}
						}
						for(int k = 0; k < terms.size(); k++){//busca cada argumento de cada termo da função
							CalculatedFunctionTermType qftt = terms.get(k);
							ContextArgumentType cat = argTable.getArgument(qftt.getCpId());
							decisionMatrix.get(i).getMatrix()[c][k] = Double.parseDouble(cat.getValue().getValue());
						}
						CalculatedArgumentType qat = new CalculatedArgumentType();
						qat.setCpId(calcpt.getCalcId());
						function.setCpId(qat.getCpId());
						function.setCcId(cc.getCcId());
						calcpt.setCalculatedArgument(qat);
						cc.getRankingArguments().add(qat);
						count++;
					}
				} catch (Exception e) {
					LogHandler.getLogger().info("Error while getting function. "+e.getCause());
					e.printStackTrace();
				}
			}
		}


		for(int i=0; i < rankParametersCount; i++){

			if(Resolution.FLOATRANK){
				double[] r = decisionMatrix.get(i).evalTOPSISVectorValue();
				for(int j=0; j < cl.getCandidate().size();j++){
					cl.getCandidate().get(j).getRankingArguments().get(i).setValue((double) r[j]);
				}
			}else{
				int[] r = decisionMatrix.get(i).evalTOPSISVector();
				for(int j=0; j < cl.getCandidate().size();j++){
					cl.getCandidate().get(j).getRankingArguments().get(i).setValue((double) r[j]);
				}
			}

		}
		return count;
	}



	//	public static int calulateRankArguments(ContextContract cc, ArgumentTable argTable, Hashtable <Integer , MaxElement> maximum, int numOfAlternatives, int alternative) throws FunctionException{
	//		int count = 0;
	//		String str = "Candidate: "+cc.getCcName()+"\n";
	//		List<CalculatedParameterType> calcps = ResolutionNode.resolutionTree.findNode(cc.getAbstractComponent().getIdAc()).getRps();
	//
	//		//######################## begin
	//		if(numOfAlternatives > 0 && decisionMatrix.size() == 0){
	//			//testar esse metodo e carregar pesos e domínios do banco
	//			int numOfMatrix = calcps.size();
	//			for(int i = 0; i < numOfMatrix; i++){
	//				DecisionMatrix d = new DecisionMatrix(cc.getCcName()+" in platform "+cc.getPlatform().getPlatformContract().getCcName());
	//				decisionMatrix.add(d);
	//			}
	//		}
	//		
	//		//######################## end
	//
	//		//Begin of calculus
	//		CalculatedParameterType calcpt;
	//		for(int i = 0; i < calcps.size(); i++){//calcula cada parametro
	//			calcpt = calcps.get(i);
	//			CalculatedFunctionType function = null;
	//			
	//
	//			try {
	//				function = CalculatedArgumentHandler.getCalculatedFunction(calcpt.getCalcId(), cc.getCcId(), ContextParameterHandler.RANKING);
	//				Hashtable<Integer, Float> weight = new Hashtable<>();
	//				ArrayList<CalculatedFunctionTermType> terms = CalculatedArgumentHandler.getCalculatedFunctionParameter(function.getFunctionId());
	//				
	//				if(function != null){
	//					
	//					//######################## begin
	//					if(decisionMatrix.get(i).getMatrix()==null){
	//						if(terms.size() > 0){
	//							for(int j = 0; j < terms.size(); j++){
	//								ContextParameterType cpt = ContextParameterHandler.getContextParameter(terms.get(j).getCpId());
	//								decisionMatrix.get(i).addCriterion(cpt.getKind(), terms.get(j).getWeight(), terms.get(j).getCpId(), cpt.getName());
	//							}
	//							decisionMatrix.get(i).createMatrix(numOfAlternatives, terms.size());
	//						}
	//					}
	//					//######################## end
	//					
	//					
	//					str+=("--"+calcpt.getName()+" = ");
	//					
	//					for(int k = 0; k < terms.size(); k++){//busca cada argumento de cada termo da função
	//						CalculatedFunctionTermType qftt = terms.get(k);
	//						ContextArgumentType cat = argTable.getArgument(qftt.getCpId());
	//						//######################## begin
	//						decisionMatrix.get(i).getMatrix()[alternative][k] = Double.parseDouble(cat.getValue().getValue());
	//						//######################## end
	//						ContextParameterType x = ContextParameterHandler.getContextParameter(qftt.getCpId());
	//						if(cat!=null){
	//							str+=(Double.parseDouble(cat.getValue().getValue())+" [ "+qftt.getWeight()+" ( "+maximum.get(qftt.getCpId()).getValue()+" ) "+" ] "+" ");
	//							
	//							if(maximum.get(qftt.getCpId())!=null){
	////								if(x.getKind()==ContextParameterHandler.INCREASEKIND){
	////									if(cat.getValue().getValue()!=null){
	////										if(maximum.get(qftt.getCpId()).getCount() > 0){
	////											cat.getValue().setValue(""+Double.parseDouble(cat.getValue().getValue())/maximum.get(qftt.getCpId()).getValue());
	////										}else{
	////											cat.getValue().setValue("1");
	////										}
	////									}else{
	////										cat.getValue().setValue("0");
	////									}
	////								}else{
	////									if(x.getKind()==ContextParameterHandler.DECREASEKIND){
	////										if(cat.getValue().getValue()!=null){
	////											if(maximum.get(qftt.getCpId()).getCount() > 0){
	////												Double m = Double.parseDouble(cat.getValue().getValue())/maximum.get(qftt.getCpId()).getValue();
	//////												m = 1-m;
	////												cat.getValue().setValue(""+m);
	////											}else{
	////												cat.getValue().setValue("1");
	////											}
	////										}else{
	////											cat.getValue().setValue("0");
	////										}
	////									}
	////								}
	//							}else{
	//								cat.getValue().setValue("1");
	//							}
	////							str+=(Double.parseDouble(cat.getValue().getValue())+" [ "+qftt.getWeight()+" ( "+maximum.get(qftt.getCpId()).getValue()+" ) "+" ] "+" ");
	//							function.getFunctionArguments().add(cat);
	//						}
	//					}
	//					str+=(" = ");
	//					CalculatedArgumentType qat = new CalculatedArgumentType();
	//					qat.setCpId(calcpt.getCalcId());
	//					function.setCpId(qat.getCpId());
	//					function.setCcId(cc.getCcId());
	//					qat.setValue(CalculatedArgumentHandler.calculate(function, weight));
	//					str+=(qat.getValue()+"\n");
	//					argTable.addNewArgument(calcpt.getCalcId(), ""+qat.getValue(), calcpt.getKindId());
	//					calcpt.setCalculatedArgument(qat);
	//
	//					cc.getRankingArguments().add(qat);
	//					count++;
	//				}
	//			} catch (Exception e) {
	//				LogHandler.getLogger().info("Error while getting function. "+e.getCause());
	//				e.printStackTrace();
	//			}
	//		}
	//		if(cc.getPlatform()!=null){
	//			count+=calulateRankArguments(cc.getPlatform().getPlatformContract(), argTable, maximum, numOfAlternatives, alternative);
	//		}
	//		try {
	//			Files.write(Paths.get("/home/wagner/Core/verifyRank.log"), str.getBytes(),StandardOpenOption.APPEND);
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	////		System.out.println(str);
	//		return count;
	//	}


	/**
	 * Given a calculated function, this method evaluate the function
	 * @param qft
	 * @return result
	 */
	public static Double calculate(CalculatedFunctionType qft, Hashtable<Integer, Float> weight){
		BigDecimal result = null;

		int numOfarguments = qft.getFunctionArguments().size();
		Expression expression = new Expression(qft.getFunctionValue());
		String f = expression.getExpression();

		for(int i = 0; i < numOfarguments; i++){
			if(qft.getFunctionArguments().get(i).getValue().getValue()==null){
				return null;
			}
			//			expression.with("v"+i, (qft.getFunctionArguments().get(i).getValue().getValue()));
			expression.setVariable("v"+i, (qft.getFunctionArguments().get(i).getValue().getValue()));
			try{
				f=f.replaceAll("v"+i, ""+(qft.getFunctionArguments().get(i).getValue().getValue()));

			}catch (NullPointerException e) {
				System.out.println("Erro ao buscar argumento, possivelmente um termo não foi encontrado na função");
				e.printStackTrace();
			}

			//Modificando os pesos para virem do banco e assim poder gerar a matriz de 
			//    alternativas e poder usar qualquer biblioteca de MCDM
			if(qft.getFunctionArguments().get(i).getContextParameter().getCpId() !=null){
				//erro, faunções de qualidade e custo nao tem peso entao devem ser bloqueadas no processo de carregar os pesos
				//incluir aqui a criação da matriz de alternativas e pesos para imprimir ou repassar a outras bibliotecas
			}
			//for(CalculatedFunctionTermType c: qft.getFunctionParameters()){
			//if(c.getCpId()==qft.getFunctionArguments().get(i).getCpId()){
			//expression.with("w"+i, (qft.getFunctionParameters().get(i).getWeight()+""));
			//}
			//}
		}

		expression.setPrecision(20);
		//		System.out.println("cc="+qft.getCcId()+"--cp="+qft.getCpId()+">>>>"+f.replace(".",","));

		//				if(qft.getCpId().equals(235)){
		//					String str=">>>>>>>>>"+expression.getExpression();
		//					for(int i=0; i < qft.getFunctionArguments().size();i++){
		//						str=str.replaceAll("v"+i, ""+qft.getFunctionArguments().get(i).getValue().getValue());
		//					}
		//					System.out.println(str);
		//				}

		result = expression.eval();
		return result.doubleValue();
	}
}


@SuppressWarnings("rawtypes")
class ComparatorCalculatedTerms implements Comparator {  
	boolean crescente = true;  

	public ComparatorCalculatedTerms(boolean crescente) {  
		this.crescente = crescente;  
	}  

	public int compare(Object o1, Object o2) {  
		CalculatedFunctionTermType p1 = (CalculatedFunctionTermType) o1;  
		CalculatedFunctionTermType p2 = (CalculatedFunctionTermType) o2;  
		if (crescente) {  
			return p1.getOrder() < p2.getOrder() ? -1 : (p1.getOrder() > p2.getOrder() ? +1 : 0);  
		} else {  
			return p1.getOrder() < p2.getOrder() ? +1 : (p1.getOrder() > p2.getOrder() ? -1 : 0);  
		}  
	}  
}  