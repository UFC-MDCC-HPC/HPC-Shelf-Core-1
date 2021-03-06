package br.ufc.storm.mcdm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngineException;
import org.rosuda.REngine.RList;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RserveException;

public class DecisionMatrix {
	int sequence;
	private String alternative_name;
	private ArrayList<Criterion> criterionList = new ArrayList<>();
	private double list [][];
	RConnection c;


	//	Pesos 4/7, 2/7 e 1/7


	public static void main(String [] a){

		int i[] = new int[5];
		i[0] = 1;
		i[1] = 51;
		i[2] = 52;
		i[3] = 53;
		i[4] = 54;
		System.out.println(findElement1(i));

		//		double m[][] = new double[5][2];
		//		m[0][0] = 1;
		//		m[0][1] = 2;
		//		m[1][0] = 3;
		//		m[1][1] = 4;
		//		m[2][0] = 5;
		//		m[2][1] = 6;
		//		m[3][0] = 7;
		//		m[3][1] = 8;
		//		m[4][0] = 9;
		//		m[4][1] = 10;
		//		
		//		double x[][]=copyMatrix(m);
		//		x=delMatrixRow(2, x);
		//		for(int i =0; i < m.length-1; i++){
		//			for(int j = 0; j < m[0].length; j++){
		//				System.out.print(x[i][j]+" ");
		//			}
		//			System.out.println();
		//		}
		//		

		//		DecisionMatrix d = new DecisionMatrix("TesteA");
		//		d.list = new double [3][3];
		//		d.criterionList.add(new Criterion(5, (float) 0.3, 150, "Criterio1"));
		//		d.criterionList.add(new Criterion(5, (float) 0.3, 151, "Criterio2"));
		//		d.criterionList.add(new Criterion(5, (float) 0.4, 152, "Criterio3"));
		//		int k=0;
		//		for(int i = 0; i < 3; i++){
		//			for(int j = 0; j < 3; j++){
		//				d.list[i][j] = k;
		//				System.out.print(k+" ");
		//				k++;
		//			}
		//			System.out.println("");
		//		}
		//		d.evalMMOORA();
		//		d.evalTOPSISVector();
		//		System.out.println(d.toRmcdmFunction("TOPSISLinear", null));
	}

	public DecisionMatrix(String alternative_name){
		this.alternative_name = alternative_name;
	}


	public String getCriteriaName(int index){
		return criterionList.get(index).getName();
	}
	public double[][] getMatrix(){
		return list;
	}

	public  ArrayList<Criterion> getCriterionList(){
		return criterionList;
	}

	public String getName(){
		return alternative_name;
	}

	public float[] getWeight(){
		float x[] = new float[criterionList.size()];
		for(int i=0; i < x.length; i++){
			x[i]=criterionList.get(i).getWeight();
		}
		return x;
	}

	public int[] evalMMOORA(){
		return evalMCDMmethod("MMOORA", "MultiMooraRanking", null);
	}

	public int[] evalTOPSISLinear(){
		return evalMCDMmethod("TOPSISLinear", "Ranking", null);
	}

	public int[] evalTOPSISVector(){
		return evalMCDMmethod("TOPSISVector", "Ranking", null);
	}

	public int[] evalVIKOR(float q){
		return evalMCDMmethod("VIKOR", "Ranking", q);
	}

	public int[] evalWASPAS(float q){
		return evalMCDMmethod("WASPAS", "Ranking", q);
	}



	//TODO: autogenerated
	public double[][] vectorNormalizeMatrix(){
		return list;

	}

	//TODO: autogenerated
	public double[][] linearNormalizeMatrix(){
		return list;

	}

	public double[] evalTOPSISLinearValue(){
		return evalMCDMmethodDoubles("TOPSISLinear", 1, null);
	}

	public double[] evalTOPSISVectorValue(){
		return evalMCDMmethodDoubles("TOPSISVector", 1, null);
	}

	public double[] evalVIKORValue(float q){
		return evalMCDMmethodDoubles("VIKOR", 2, q);
	}


	public double[] evalWASPASValue(float q){
		return evalMCDMmethodDoubles("WASPAS", 2, q);
	}


	public int[] evalRelativeMCDMmethod(String method, String column, Float q, double newlist[][]){
		REXP rResponseObject=null;
		double sum=0;
		//Inclusão da avaliação de matrizes com um único critério
		//Se entrar nesse caso, basta retornar a propria matriz
		if(criterionList.size()==1){
			int[] r = rankVector(newlist);
			return r;
		}
		for(int i=0; i < criterionList.size();i++){
			sum+=Double.parseDouble(criterionList.get(i).getWeight()+"");
		}
		double w [] = new double[criterionList.size()];
		if(criterionList.size()==1){
			int[] r = rankVector(list);
			return r;
		}
		String cb [] = new String[criterionList.size()];
		try {
			c = new RConnection();
			c.voidEval("library(MCDM)");
			c.assign("d", REXP.createDoubleMatrix(newlist));
			for(int i = 0; i < criterionList.size(); i++){
				w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");
				if(criterionList.get(i).getDomain()==5){
					cb[i]="min";
				}else{
					cb[i]="max";
				}
			}
			c.assign("w", w);
			c.assign("cb", cb);


			if (q==null) {
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb)"+"),silent=TRUE)");
			}else{
				c.voidEval("v<-"+q);
				//				c.assign("v", q.toString());
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb, v)"+"),silent=TRUE)");
			}

			if (rResponseObject.inherits("try-error")) { 
				System.out.println(rResponseObject.asString());; 
			}
			RList l = rResponseObject.asList();
			int[] ret = l.at(column).asIntegers();
			//			System.out.println(method+" Ranking");
			//			for(Integer i: ret){
			//				System.out.println(i);
			//			}
			//			System.out.println("----------------------");

			//			System.out.println("AAAAA______________________\n"+toRmcdmFunction(method, q)+"\n___________________________\n");
			return ret;
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public int[] evalMCDMmethod(String method, String column, Float q){
		REXP rResponseObject=null;
		double sum=0;
		//Inclusão da avaliação de matrizes com um único critério
		//Se entrar nesse caso, basta retornar a propria matriz
		if(criterionList.size()==1){
			int[] r = rankVector(list);
			
			return r;
		}
		//
		for(int i=0; i < criterionList.size();i++){
			sum+=Double.parseDouble(criterionList.get(i).getWeight()+"");
		}
		double w [] = new double[criterionList.size()];
		String cb [] = new String[criterionList.size()];
		try {
			c = new RConnection();
			c.voidEval("library(MCDM)");
			c.assign("d", REXP.createDoubleMatrix(list));
			for(int i = 0; i < criterionList.size(); i++){
				w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");
				if(criterionList.get(i).getDomain()==5){
					cb[i]="min";
				}else{
					cb[i]="max";
				}
			}
			c.assign("w", w);
			c.assign("cb", cb);


			if (q==null) {
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb)"+"),silent=TRUE)");
			}else{
				c.voidEval("v<-"+q);
				//				c.assign("v", q.toString());
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb, v)"+"),silent=TRUE)");
			}

			if (rResponseObject.inherits("try-error")) { 
				System.out.println(rResponseObject.asString());; 
			}
			RList l = rResponseObject.asList();
			int[] ret = l.at(column).asIntegers();
			//			System.out.println(method+" Ranking");
			//			for(Integer i: ret){
			//				System.out.println(i);
			//			}
			//			System.out.println("----------------------");
			VerifyRank(method, column, q, list);
			//			System.out.println("______________________\n"+toRmcdmFunction(method, null)+"\n___________________________\n");
			return ret;
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public double[] evalMCDMmethodDoubles(String method, int column, Float q){
		REXP rResponseObject=null;
		double sum=0;
		//Inclusão da avaliação de matrizes com um único critério
		//Se entrar nesse caso, basta retornar a propria matriz

		if(criterionList.size()==1){
			double r[] = new double[list.length];
			int[] x = rankVector(list);;
			for(int i=0; i < list.length; i++){
				r[i]=x[i];
			}
			return r;
		}

		for(int i=0; i < criterionList.size();i++){
			sum+=Double.parseDouble(criterionList.get(i).getWeight()+"");
		}
		double w [] = new double[criterionList.size()];
		String cb [] = new String[criterionList.size()];
		try {
//			InvokeRserve.invoke();
			c = new RConnection();
			c.voidEval("library(MCDM)");
			c.assign("d", REXP.createDoubleMatrix(list));
			for(int i = 0; i < criterionList.size(); i++){
				w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");
				if(criterionList.get(i).getDomain()==5){
					cb[i]="min";
				}else{
					cb[i]="max";
				}
			}
			c.assign("w", w);
			c.assign("cb", cb);


			if (q==null) {
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb)"+"),silent=TRUE)");
			}else{
				c.voidEval("v<-"+q);
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb, v)"+"),silent=TRUE)");
			}

			if (rResponseObject.inherits("try-error")) { 
				System.out.println(rResponseObject.asString());; 
			}
			//			System.out.println("______________________\n"+toRmcdmFunction(method, q)+"\n___________________________\n");
			RList l = rResponseObject.asList();
			double[] ret = l.at(column).asDoubles();
			//			System.out.println(method+" Ranking");
			//			for(Integer i: ret){
			//				System.out.println(i);
			//			}
			//			System.out.println("----------------------");
			//			VerifyRank(method, column, q, list);

			return ret;
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
//TODO:Concluir
	public double[] evalPrometheeDoubles(String method, int column, Float q){
		REXP rResponseObject=null;
		double sum=0;
		//Inclusão da avaliação de matrizes com um único critério
		//Se entrar nesse caso, basta retornar a propria matriz

		if(criterionList.size()==1){
			double r[] = new double[list.length];
			int[] x = rankVector(list);;
			for(int i=0; i < list.length; i++){
				r[i]=x[i];
			}
			return r;
		}

		for(int i=0; i < criterionList.size();i++){
			sum+=Double.parseDouble(criterionList.get(i).getWeight()+"");
		}
		double w [] = new double[criterionList.size()];
		String cb [] = new String[criterionList.size()];
		try {
			c = new RConnection();
			c.voidEval("library(MCDM)");
			c.assign("d", REXP.createDoubleMatrix(list));
			for(int i = 0; i < criterionList.size(); i++){
				w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");
				if(criterionList.get(i).getDomain()==5){
					cb[i]="min";
				}else{
					cb[i]="max";
				}
			}
			c.assign("w", w);
			c.assign("cb", cb);


			if (q==null) {
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb)"+"),silent=TRUE)");
			}else{
				c.voidEval("v<-"+q);
				rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb, v)"+"),silent=TRUE)");
			}

			if (rResponseObject.inherits("try-error")) { 
				System.out.println(rResponseObject.asString());; 
			}
			//			System.out.println("______________________\n"+toRmcdmFunction(method, q)+"\n___________________________\n");
			RList l = rResponseObject.asList();
			double[] ret = l.at(column).asDoubles();
			//			System.out.println(method+" Ranking");
			//			for(Integer i: ret){
			//				System.out.println(i);
			//			}
			//			System.out.println("----------------------");
			//			VerifyRank(method, column, q, list);

			return ret;
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public int[] evalLexicographic(){
		REXP rResponseObject=null;
		try {
			c = new RConnection();
			c.assign("d", REXP.createDoubleMatrix(list));
			String exp="try(order(";
			for(int i=1; i <= criterionList.size(); i++){
				exp+="d[,"+i+"],";
			}
			exp+=" na.last = TRUE, decreasing = FALSE))";
			//			System.out.println(exp);
			rResponseObject = c.parseAndEval(exp);

			if (rResponseObject.inherits("try-error")) { 
				System.out.println(rResponseObject.asString());; 
			}
			//			RList l = rResponseObject.asList();
			int[] ret = rResponseObject.asIntegers();
//			System.out.println("Lexicographi Order Ranking");
//			for(Integer i: ret){
//				System.out.println(i);
//			}
//			System.out.println("----------------------");

			return ret;
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public void VerifyRank(String method, String column, Float q, double m[][]){

		String str="";
		int p=-1;
		double matrix[][] = copyMatrix(m);
		int alternatives[] = new int[m.length]; 
		for(int x=0; x < alternatives.length; x++){
			alternatives[x]=x;
		}
		int[] res = evalRelativeMCDMmethod(method, column, q, matrix);
		p = findElement1(res);
		str+=(method+" \n"+p+"\n");
		for(int i = 0; i < m.length-1; i++){
			//			System.out.println("------------>>>"+matrix.length+" - p = "+p);
			matrix = delMatrixRow(p, matrix);
			alternatives=delArrayRow(p, alternatives);
			res = evalRelativeMCDMmethod(method, column, q, matrix);
			p = findElement1(res);
			str += alternatives[p]+"\n";
		}
		str+="\n";
		try {
			Files.write(Paths.get("pairwiseComparing.log"), str.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static int findElement1(int array[]){
		for(int i = 0; i <  array.length; i++){
			if(array[i] == 1){
				return i;
			}
		}
		return -1;
	}

	public static double [][] copyMatrix(double m[][]){
		double matrix[][] = new double[m.length][m[0].length];
		for(int i=0; i < m.length;i++){
			for(int j=0;j<m[0].length;j++){
				matrix[i][j]=m[i][j];
			}
		}
		return matrix;
	}

	public static double [][] delMatrixRow(int pos, double m[][]){
		double matrix[][] = new double[m.length-1][m[0].length];
		int row = 0;
		for(int i = 0; i < m.length;i++){
			for(int j = 0; j < m[0].length; j++){
				if(i != pos){
					matrix[row][j] = m[i][j];
				}
			}
			if(i != pos){
				row++;
			}
		}
		return matrix;
	}
	public static int [] delArrayRow(int pos, int m[]){
		int matrix[] = new int[m.length-1];
		int row = 0;
		for(int i = 0; i < m.length;i++){
			if(i != pos){
				matrix[row] = m[i];
			}
			if(i != pos){
				row++;
			}
		}
		return matrix;
	}
	/*	
	public int[] evalMCDMmethod(String method, String column){
		double sum=0;
		//Inclusão da avaliação de matrizes com um único critério
		//Se entrar nesse caso, basta retornar a própŕia matriz
		if(criterionList.size()==1){
			int r[] = rankVector(list);
			return r;
		}
		//
		for(int i=0; i < criterionList.size();i++){
			sum+=Double.parseDouble(criterionList.get(i).getWeight()+"");
		}
		double w [] = new double[criterionList.size()];
		String cb [] = new String[criterionList.size()];
		try {
			c = new RConnection();
			c.voidEval("library(MCDM)");
			c.assign("d", REXP.createDoubleMatrix(list));
			for(int i = 0; i < criterionList.size(); i++){
				w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");
				if(criterionList.get(i).getDomain()==5){
					cb[i]="min";
				}else{
					cb[i]="max";
				}
			}
			c.assign("w", w);
			c.assign("cb", cb);
			REXP rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb)"+"),silent=TRUE)"); 
			if (rResponseObject.inherits("try-error")) { 
				System.out.println(rResponseObject.asString());; 
			}
			RList l = rResponseObject.asList();
			int[] ret = l.at(column).asIntegers();
			//			System.out.println(method+" Ranking");
			//			for(Integer i: ret){
			//				System.out.println(i);
			//			}
			//			System.out.println("----------------------");

			System.out.println("______________________\n"+toRmcdmFunction(method)+"\n___________________________\n");
			return ret;
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	 */
	@SuppressWarnings("unchecked")
	private int[] rankVector(double[][] list2) {
		if(list2[0].length!=1){
			return null;
		}
		List<Pair> l = new ArrayList<>();
		for(int i = 0; i < list2.length; i++){
			l.add(new Pair(i, list2[i][0]));
		}
		Collections.sort (l, new ComparatorPairs(true));
		int[] aux = new int[l.size()];

		for(int i = 0; i < l.size(); i++){
			for(int j = 0; j < l.size(); j++){
				if(l.get(j).getPos()==i){
					aux[i]=j;
				}
			}
		}
		return aux;
	}
	
	@SuppressWarnings("unchecked")
	private double[] rankVectorDouble(double[][] list2) {
		if(list2[0].length!=1){
			return null;
		}
		List<Pair> l = new ArrayList<>();
		for(int i = 0; i < list2.length; i++){
			l.add(new Pair(i, list2[i][0]));
		}
		Collections.sort (l, new ComparatorPairs(true));
		double[] aux = new double[l.size()];

		for(int i = 0; i < l.size(); i++){
			for(int j = 0; j < l.size(); j++){
				if(l.get(j).getPos()==i){
					aux[i]=(double) j;
				}
			}
		}
		return aux;
	}

	/*public int[] evalMCDMmethod(String method, int column){

		double w [] = new double[criterionList.size()];
		String cb [] = new String[criterionList.size()];
		try {
			c = new RConnection();
			c.voidEval("library(MCDM)");
			c.assign("d", REXP.createDoubleMatrix(list));
			for(int i = 0; i < criterionList.size(); i++){
				w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");

				if(criterionList.get(i).getDomain()==5){
					cb[i]="min";
				}else{
					cb[i]="max";
				}
			}
			c.assign("w", w);
			c.assign("cb", cb);
			REXP rResponseObject = c.parseAndEval("try(eval("+method+"(d,w,cb)"+"),silent=TRUE)"); 
			if (rResponseObject.inherits("try-error")) { 
				System.out.println(rResponseObject.asString());; 
			}
			RList l = rResponseObject.asList();
			int[] ret = l.at(column).asIntegers();
			System.out.println(method+" Ranking");
			for(Integer i: ret){
				System.out.println(i);
			}
			System.out.println("----------------------");
			return l.at(column).asIntegers();
		} catch (RserveException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REXPMismatchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (REngineException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}*/


	public static String toString(double m[][]){
		String s = "";
		for(int i = 0; i < m.length; i++){
			for(int j = 0; j < m[0].length; j++){
				s+=m[i][j];
				if(j != m[0].length-1){
					s+=", ";
				}
			}
			s+="\n";
		}
		return s;
	}

	public String toString(){
		String str = "";
		int rows = list.length;
		int columns = criterionList.size();
		str+="Decision Matrix of component: "+alternative_name+"\n";
		str+="\n| ";
		for(int k = 0; k < columns; k++){
			if(criterionList.get(k).getDomain()==5){
				str+= criterionList.get(k).getName()+"["+criterionList.get(k).getWeight()+"](DECREASING) | ";
			}else{
				str+= criterionList.get(k).getName()+"["+criterionList.get(k).getWeight()+"](INCREASING) | ";
			}
		}
		str+="\n\n";
		for(int i = 0; i < rows; i++){
			str+=i+" & ";
			for(int j = 0; j < columns; j++){
				str+= getValue(i,j)+" & ";
			}
			str+="\n";
		}
		return str;

	}


	public String toRmcdmFunction(String func, Float q){
		String str = "library(MCDM)\n";
		str+="d<-matrix(c(";
		int rows = list.length;
		int columns = criterionList.size();

		for(int j = 0; j < columns; j++){
			for(int i = 0; i < rows; i++){
				str+= getValue(i,j);
				if(i<rows-1 || j < columns-1){
					str+=", ";
				}
			}
		}
		str+="),nrow = "+rows+",ncol = "+columns+")\n";
		str+="w <- c(";
		for(int k = 0; k < columns; k++){
			str+=criterionList.get(k).getWeight();
			if(k < columns-1){
				str+=", ";
			}
		}
		str+=")\n";
		str+="cb <- c(";
		for(int k = 0; k < columns; k++){
			if(criterionList.get(k).getDomain()==5){
				str+="\'min\'";
			}else{
				str+="\'max\'";
			}

			if(k < columns-1){
				str+=", ";
			}
		}
		str+=")\n";
		if(q!=null){
			str+="v<-"+q+"\n";
			str+=func+"(d, w, cb, v)";
		}else{
			str+=func+"(d, w, cb)";
		}

		return str;
	}

	public String toPromethee(String func, Float q, String function){
		String str = "library(PROMETHEE)\n";
		str+="d<-matrix(c(";
		int rows = list.length;
		int columns = criterionList.size();

		for(int j = 0; j < columns; j++){
			for(int i = 0; i < rows; i++){
				str+= getValue(i,j);
				if(i<rows-1 || j < columns-1){
					str+=", ";
				}
			}
		}
		str+="),nrow = "+rows+",ncol = "+columns+")\n";

		//=====================================================

		str+="w<-as.data.frame(rbind(";
		for(int i = 0; i < rows; i++){
			str+="c(";
			for(int j = 0; j < columns; j++){
				str+=criterionList.get(j).getWeight();
				if(j < columns-1){
					str+=", ";
				}else{
					if(i < rows-1){
						str+="),";
					}else{
						str+=")";
					}
					
				}
			}
		}
		str+="))\n";

		//==============================
		str+="cb<-as.data.frame(rbind(";
		for(int i = 0; i < rows; i++){
			str+="c(";
			for(int j = 0; j < columns; j++){
				if(criterionList.get(j).getDomain()==5){
					str+="\'min\'";
				}else{
					str+="\'max\'";
				}
				if(j < columns-1){
					str+=", ";
				}else{
					if(i < rows-1){
						str+="),";
					}else{
						str+=")";
					}
					
				}
			}
		}
		str+="))\n";
		
		//===============================

		str+="PreT<-as.data.frame(rbind(";
		for(int i = 0; i < rows; i++){
			str+="c(";
			for(int j = 0; j < columns; j++){
				str+="0";
				if(j < columns-1){
					str+=", ";
				}else{
					if(i < rows-1){
						str+="),";
					}else{
						str+=")";
					}
					
				}
			}
		}
		str+="))\n";
		
		//========================

		str+="IndT<-as.data.frame(rbind(";
		for(int i = 0; i < rows; i++){
			str+="c(";
			for(int j = 0; j < columns; j++){
				str+="0";
				if(j < columns-1){
					str+=", ";
				}else{
					if(i < rows-1){
						str+="),";
					}else{
						str+=")";
					}
					
				}
			}
		}
		str+="))\n";
		
		//=====================================

		str+="gauss<-as.data.frame(rbind(";
		for(int i = 0; i < rows; i++){
			str+="c(";
			for(int j = 0; j < columns; j++){
				str+="0";
				if(j < columns-1){
					str+=", ";
				}else{
					if(i < rows-1){
						str+="),";
					}else{
						str+=")";
					}
					
				}
			}
		}
		str+="))\n";
		//=====================================

		str+="PreF<-as.data.frame(rbind(";
		for(int i = 0; i < rows; i++){
			str+="c(";
			for(int j = 0; j < columns; j++){
				str+="\""+function+"\"";
				if(j < columns-1){
					str+=", ";
				}else{
					if(i < rows-1){
						str+="),";
					}else{
						str+=")";
					}
					
				}
			}
		}
		str+="))\n";
		
//		str+=")\n";
		str+=func+"(d, PreF,PreT,IndT,w,cb,gauss)";
		return str;
	}

	public String toAHP(){
		String tab = "  ";
		String str = "Version: 1.0 \n\n######################### \n# Alternatives Section\n#\n\nAlternatives: &alternatives\n";
		int rows = list.length;
		int columns = criterionList.size();
		for(int i = 0; i < rows; i++){
			str+=tab+"Alternative"+i+":\n";
			for(int j = 0; j < columns; j++){
				str+=tab+tab+criterionList.get(j).getName()+": "+getValue(i,j)+"\n";
			}
			//			str+="\n";
		}
		str+="#\n# End of Alternatives Section\n#####################################\n\n#####################################\n# Goal Section\n#\n\n\n";
		str+="Goal:\n";
		str+=tab+"name: "+alternative_name+"\n";
		str+=tab+"preferences: \n";
		str+=tab+tab+"priority: \n";
		for(int i = 0; i < criterionList.size(); i++){
			str+=tab+tab+tab+"- "+criterionList.get(i).getName()+": "+criterionList.get(i).getWeight()+"\n";
		}
		//		 preferences:
		//			    # preferences are typically defined pairwise
		//			    # 1 means: A is equal to B
		//			    # 9 means: A is highly preferrable to B
		//			    # 1/9 means: B is highly preferrable to A
		//			    pairwise:
		//			      - [Cost, Safety, 3]
		//			      - [Cost, Style, 7]
		//			      - [Cost, Capacity, 3]
		//			      - [Safety, Style, 9]
		//			      - [Safety, Capacity, 1]
		//			      - [Style, Capacity, 1/7]
		//		str+=tab+tab+"children: \n";
		//		for(int i = 0; i < criterionList.size(); i++){
		//			str+=tab+tab+tab+criterionList.get(i).getName()+"\n";
		//		}
		str+=tab+"children: *alternatives\n\n#\n# End of Goal Section\n#####################################";
		return str;

	}



	double getValue(int sequence, int criterion){
		try{
			return list[sequence][criterion];
		}catch (Exception e) {
			// TODO: handle exception
		}
		return 0;
	}

	double getValueByCpId(int cp_id, int sequence){
		int seq = -1;
		for(Criterion c:criterionList){
			if(c.getCp_id()==cp_id){
				seq = c.getPosition();
			}
		}
		try{
			return list[sequence][seq];
		}catch (Exception e) {
			// TODO: handle exception
		}
		return 0;
	}

	public boolean addCriterion(int domain, float weight, int cp_id, String name){
		try {
			criterionList.add(new Criterion(domain, weight, cp_id, name));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void createMatrix(int numOfAlternatives, int numOfCriterion) {
		list = new double [numOfAlternatives][numOfCriterion];
	}

	//	public double[][] getNormalizedMatrix(){
	//	double matrix[][] = list;
	//	double max = 0;
	//	double min =0;
	//	for(int i = 0; i < matrix.length; i++){
	//		for(int j = 0; j < matrix[0].length; j++){
	//			if(matrix[i][j]>max){
	//				max=matrix[i][j];
	//			}
	//			if(matrix[i][j]< min){
	//				min = matrix[i][j];
	//			}
	//			
	//		}
	//		
	//		for(int j = 0; j < matrix[0].length; j++){
	//			if(criterionList.get(j).getDomain()==5.0){
	//				matrix[i][j]=(matrix[i][j]*min)/matrix[i][j];
	//			}else{
	//				matrix[i][j]=matrix[i][j]/(max*matrix[i][j]);
	//			}
	//		}
	//	}
	//	return matrix;
	//}

	/*public int[] evalVIKOR(float q){
	if(criterionList.size()==1){
		int r[] = rankVector(list);
		return r;
	}
	double w [] = new double[criterionList.size()];
	String cb [] = new String[criterionList.size()];
	try {
		c = new RConnection();
		c.voidEval("library(MCDM)");
		c.assign("d", REXP.createDoubleMatrix(list));
		for(int i = 0; i < criterionList.size(); i++){
			w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");
			if(criterionList.get(i).getDomain()==5){
				cb[i]="min";
			}else{
				cb[i]="max";
			}
		}
		c.assign("w", w);
		c.assign("cb", cb);
		c.voidEval("v<-"+q);// assign("v", (double) q);
		REXP rResponseObject = c.parseAndEval("try(eval(VIKOR(d, w, cb,v)"+"),silent=TRUE)"); 
		if (rResponseObject.inherits("try-error")) { 
			System.out.println(rResponseObject.asString());; 
		}
		RList l = rResponseObject.asList();
		int[] ret = l.at("Ranking").asIntegers();

//		String s = "";
//		for(int x=0; x < cb.length; x++){
//			s+=cb[x]+", ";
//		}
//		System.out.println(">>>>>>>>>>>>"+s);

		return ret;
	} catch (RserveException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (REXPMismatchException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (REngineException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return null;
}*/

	/*public int[] evalWASPAS(float q){
	if(criterionList.size()==1){
		int r[] = rankVector(list);
		return r;
	}
	double w [] = new double[criterionList.size()];
	String cb [] = new String[criterionList.size()];
	try {
		c = new RConnection();
		c.voidEval("library(MCDM)");
		c.assign("d", REXP.createDoubleMatrix(list));
		for(int i = 0; i < criterionList.size(); i++){
			w[i]=Double.parseDouble(criterionList.get(i).getWeight()+"");
			if(criterionList.get(i).getDomain()==5){
				cb[i]="min";
			}else{
				cb[i]="max";
			}
		}
		c.assign("w", w);
		c.assign("cb", cb);
		c.voidEval("v<-"+q);// assign("v", (double) q);
		REXP rResponseObject = c.parseAndEval("try(eval(WASPAS(d, w, cb,v)"+"),silent=TRUE)"); 
		if (rResponseObject.inherits("try-error")) { 
			System.out.println(rResponseObject.asString());; 
		}
		RList l = rResponseObject.asList();
		int[] ret = l.at("Ranking").asIntegers();
		return ret;
	} catch (RserveException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (REXPMismatchException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (REngineException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return null;
}*/

}

class Pair{
	private int pos;
	private int newpos;
	private double value;
	public Pair(int p, double v) {
		pos=p;
		value=v;
	}
	public int getPos(){
		return pos;
	}

	public double getValue(){
		return value;
	}

	public int getNewPos(){
		return newpos;
	}

	public void setPos(int p){
		pos=p;
	}

	public void setValue(double v){
		value=v;
	}

	public void setNewPos(int p){
		newpos=p;
	}
}

@SuppressWarnings("rawtypes")
class ComparatorPairs implements Comparator {  
	boolean crescente = true;  

	public ComparatorPairs(boolean crescente) {  
		this.crescente = crescente;  
	}  

	public int compare(Object o1, Object o2) {  
		Pair p1 = (Pair) o1;  
		Pair p2 = (Pair) o2;  
		if (crescente) {  
			return p1.getValue() < p2.getValue() ? -1 : (p1.getValue() > p2.getValue() ? +1 : 0);  
		} else {  
			return p1.getValue() < p2.getValue() ? +1 : (p1.getValue() > p2.getValue() ? -1 : 0);  
		}  
	}  
}  