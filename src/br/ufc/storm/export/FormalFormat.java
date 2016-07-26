package br.ufc.storm.export;

import br.ufc.storm.exception.DBHandlerException;
import br.ufc.storm.jaxb.AbstractComponentType;
import br.ufc.storm.jaxb.CalculatedArgumentType;
import br.ufc.storm.jaxb.CalculatedParameterType;
import br.ufc.storm.jaxb.ContextArgumentType;
import br.ufc.storm.jaxb.ContextContract;
import br.ufc.storm.jaxb.ContextParameterType;
import br.ufc.storm.jaxb.CostParameterType;
import br.ufc.storm.jaxb.QualityParameterType;
import br.ufc.storm.sql.AbstractComponentHandler;
import br.ufc.storm.sql.ContextArgumentHandler;
import br.ufc.storm.sql.ContextContractHandler;
import br.ufc.storm.xml.XMLHandler;

public class FormalFormat {

	public static void main(String[] args) {
		try {
//			ContextContract cc = ContextContractHandler.getContextContract(126);
//			System.out.println(FormalFormat.exportContextContract(cc, null));

						System.out.println(FormalFormat.exportComponentSignature(AbstractComponentHandler.getAbstractComponent(19), null));
//						System.out.println(XMLHandler.getAbstractComponent("Cluster"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String exportComponentSignature(AbstractComponentType ac, String space){
		String str = "";
		if(space == null){
			space="    ";
		}else{
			space+="    ";
		}
		str+=ac.getName()+"=[";
		for(ContextParameterType cp: ac.getContextParameter()){
			if(cp.getBound()!= null){
				try {
					if(cp.getBound().getCcName().equals("DATA")){
						str+="\n"+space+cp.getName()+" : "+cp.getBound().getCcName()+",";
					}else{
						str+="\n"+space+cp.getName()+" : "+cp.getBound().getCcName()+" ["+exportComponentSignature(AbstractComponentHandler.getAbstractComponentFromContextContractID(cp.getBound().getCcId()), space)+"],";
					}
				} catch (DBHandlerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				str+="\n"+space+cp.getName()+" : "+cp.getBoundValue()+",";
			}
		}
		for(CalculatedParameterType cp: ac.getQualityParameters()){
			str+="\n"+space+cp.getName()+" : DATA";
		}
		for(CalculatedParameterType cp: ac.getCostParameters()){
			str+="\n"+space+cp.getName()+" : DATA";
		}
		for(CalculatedParameterType cp: ac.getRankingParameters()){
			str+="\n"+space+cp.getName()+" : DATA";
		}
		str+="]";
		return str;
	}


	public static String exportContextContract(ContextContract cc, String space){
		if(space == null){
			try {
				ContextContractHandler.completeContextContract(cc);
			} catch (DBHandlerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			space="    ";
		}else{
			space+="    ";
		}
		String str = "";
		AbstractComponentType ac = cc.getAbstractComponent();
		str+=cc.getCcName()+"[";
		for(ContextParameterType cp: ac.getContextParameter()){
			for(ContextArgumentType ca : cc.getContextArguments()){
				if(cp.getCpId()==ca.getCpId()){
					if(ca.getContextContract()!=null){
						str+="\n"+space+cp.getName()+" = "+exportContextContract(ca.getContextContract(),"    ")+",";
					}else{
						if(ca.getValue()!= null){
							str+="\n"+space+cp.getName()+" = "+ca.getValue().getValue()+",";
						}else{
							if(ca.getCpId()!= null){
								//Definir comportamento
							}
						}
					}
				}
			}
		}
		
		for(CalculatedParameterType cp: ac.getQualityParameters()){
			str+="\n"+space+cp.getName()+" = ";
			for(CalculatedArgumentType c : cc.getQualityArguments()){
				if(c.getCalcId()==cp.getCalcId()){
					str+=c.getValue();//terminar
				}
			}
		}
		for(CalculatedParameterType cp: ac.getCostParameters()){
			str+="\n"+space+cp.getName()+" = ";
			for(CalculatedArgumentType c : cc.getCostArguments()){
				if(c.getCalcId()==cp.getCalcId()){
					str+=c.getValue();//terminar
				}
			}
		}
		for(CalculatedParameterType cp: ac.getRankingParameters()){
			str+="\n"+space+cp.getName()+" = ";
			for(CalculatedArgumentType c : cc.getRankingArguments()){
				if(c.getCalcId()==cp.getCalcId()){
					str+=c.getValue();//terminar
				}
			}
		}
		str+="]";
		return str;
	}
}