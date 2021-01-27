package utils;

import static org.protelis.lang.interpreter.util.Op2.AND;
import static org.protelis.lang.interpreter.util.Op2.DIVIDE;
import static org.protelis.lang.interpreter.util.Op2.GREATER;
import static org.protelis.lang.interpreter.util.Op2.MAX;
import static org.protelis.lang.interpreter.util.Op2.MIN;
import static org.protelis.lang.interpreter.util.Op2.MINUS;
import static org.protelis.lang.interpreter.util.Op2.PLUS;
import static org.protelis.lang.interpreter.util.Op2.SMALLER;
import static org.protelis.lang.interpreter.util.Op2.TIMES;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import org.protelis.lang.datatype.DeviceUID;
import org.protelis.lang.datatype.Field;
import org.protelis.lang.datatype.JVMEntity;
import org.protelis.lang.datatype.Tuple;
import org.protelis.lang.datatype.impl.FieldMapImpl;
import org.protelis.lang.interpreter.util.Op3;
import org.protelis.vm.ExecutionContext;

import com.google.common.collect.ObjectArrays;

public class BinarySearch {
	
	private static DeviceUID uid = null;
	
	private BinarySearch() { /** Private constructor to hide public one */ }
	
	/**
	 *  public def nbrMaxDist() = self.nbrRange()		// could add rel and abs error
		public def nbrMinDist() = self.nbrRange()		// could add rel and abs error
		
		public def nbrMaxLag() = self.nbrLag()			// could add rel and abs error
		public def nbrMinLag() = self.nbrLag()			// could add rel and abs error 
		
		public def nbrMaxDistNow(W) = nbrMaxDist() + nbrMaxLag() * W
		public def nbrMinDistNow(W) = nbrMinDist() - nbrMaxLag() * W
	 * */
	private static Object nbrMaxDistNow(
			Double w, Field<?> nbrRange, Field<?> nbrLag) {
		return PLUS.run(nbrRange, TIMES.run(nbrLag, w));
	}
	
	private static Object nbrMinDistNow(
			Double w, Field<?> nbrRange, Field<?> nbrLag) {
		return MINUS.run(nbrRange, TIMES.run(nbrLag, w));
	}
	
	/**
	 * def connectionP(R, W) =
			min(max((R - nbrMinDistNow(W)) / 
					(nbrMaxDistNow(W) - nbrMinDistNow(W)), 0), 1)
	 * */
	private static Object connectionP(Double r, Double w,
			Field<?> nbrRange, Field<?> nbrLag) {
		
		Object maxDist = nbrMaxDistNow(w, nbrRange, nbrLag);
		Object minDist = nbrMinDistNow(w, nbrRange, nbrLag);
		return MIN.run(MAX.run(DIVIDE.run(
			MINUS.run(r, minDist), MINUS.run(maxDist, minDist)), 
		0), 1);
	}
	
	/**
	 * def survivalP(v, D, T, Pu, Pl) =
			min(max((D - v * max(nbr(T) - nbrMinLag(), 1E-3) - nbr(Pl)) / 
					nbr(Pu - Pl), 0), 1)
	 * */
	private static Object survivalP(Double v, Double d,
			Field<?> nbrT , Field<?> nbrLag, 
			Field<?> nbrPl, Field<?> nbrPu) { 
		
		return MIN.run(MAX.run(DIVIDE.run(
			MINUS.run(MINUS.run(d, 
				TIMES.run(v, MAX.run(MINUS.run(nbrT, nbrLag), 1E-3))), 
			nbrPl), 
		MINUS.run(nbrPu, nbrPl)), 0), 1);
	}
	
	/**
	 * def failingP(v, D, T, Pu, Pl, R, W) = 
			mux(D > nbr(D) && !isInfinite(D) && nbrMaxDistNow(W) < R) {
				1 - connectionP(R, W) * survivalP(v, D, T, Pu, Pl)
			} else { 1 }
	 * */
	public static Object failingP(Double v, Double d, Double r, Double w,
			Field<?> nbrD  , Field<?> nbrRange, Field<?> nbrT, 
			Field<?> nbrLag, Field<?> nbrPl	  , Field<?> nbrPu) {
		
		return Op3.MUX.run(AND.run(AND.run(
				GREATER.run(d, nbrD), !Double.isInfinite(d)), 
				SMALLER.run(nbrMaxDistNow(w, nbrRange, nbrLag), r)),
			MINUS.run(1, TIMES.run(
				connectionP(r, w, nbrRange, nbrLag), 
				survivalP(v, d, nbrT, nbrLag, nbrPl, nbrPu))), 1);
	}
	
	/**
	 * def failingAllP(v, D, T, Pu, Pl, R, W) = 
			foldHood(1, failingP(v, D, T, Pu, Pl, R, W), { a, b -> a * b })
	 * */
	@SuppressWarnings("unchecked")
	public static Object failingAllP(Double v, Double d, Double r, Double w,
			FieldMapImpl<?> nbrD , FieldMapImpl<?> nbrRange, 
			FieldMapImpl<?> nbrT , FieldMapImpl<?> nbrLag, 
			FieldMapImpl<?> nbrPl, FieldMapImpl<?> nbrPu) {
		
		Field<Number> failP = (Field<Number>) failingP(
			v, d, r, w, nbrD, nbrRange, nbrT, nbrLag, nbrPl, nbrPu);
		
		return failP.stream().filter(e -> !e.getKey().equals(uid))
			.mapToDouble(e -> e.getValue().doubleValue())
			.reduce(1.0, (a, b) -> a * b);
	}
	
	/**
	 * def mostReliableFailP(v, D, T, Pu, Pl, R, W) =
			foldMin(POSITIVE_INFINITY, failingP(v, D, T, Pu, Pl, R, W)) 
	 * */
	@SuppressWarnings("unchecked")
	public static Object mostReliableFailP(Double v, Double d, Double r, Double w,
			FieldMapImpl<?> nbrD , FieldMapImpl<?> nbrRange, 
			FieldMapImpl<?> nbrT , FieldMapImpl<?> nbrLag, 
			FieldMapImpl<?> nbrPl, FieldMapImpl<?> nbrPu) { 
		
		Field<Number> failP = (Field<Number>) failingP(
				v, d, r, w, nbrD, nbrRange, nbrT, nbrLag, nbrPl, nbrPu);
			
		return failP.stream().filter(e -> !e.getKey().equals(uid))
			.mapToDouble(e -> e.getValue().doubleValue())
			.min().orElse(Double.POSITIVE_INFINITY);
	}
	
	/**
	 * def expectedVariance(v, D, T, Pu, Pl, R, W, failTh) {
			let failP = max(1E-3, failingP(v, D, T, Pu, Pl, R, W))
			1 / max(1E-3, foldSum(0, mux (failP < failTh) 
				{ (1 - failP) / failP } else { 0 })) }
	 * */
	@SuppressWarnings("unchecked")
	public static Object expectedVariance(
			Double v, Double d, Double r, Double w, Double failThreshold,
			FieldMapImpl<?> nbrD , FieldMapImpl<?> nbrRange, 
			FieldMapImpl<?> nbrT , FieldMapImpl<?> nbrLag, 
			FieldMapImpl<?> nbrPl, FieldMapImpl<?> nbrPu) {
		
		Field<Number> failP = (Field<Number>) 
				MAX.run(1E-3, failingP(
			v, d, r, w, nbrD, nbrRange, nbrT, nbrLag, nbrPl, nbrPu));
		
		Double norm = ((Field<Number>) Op3.MUX.run(
			SMALLER.run(failP, failThreshold),
			DIVIDE.run(MINUS.run(1, failP), failP), 0)).stream()
		.filter(e -> !e.getKey().equals(uid))
		.mapToDouble(e -> e.getValue().doubleValue()).sum();
		
		return 1 / Math.max(1E-3, norm);
	}
	
	/**
	 * Binary searches for an optimal value.
	 * 
	 * @param ctx 		the execution context of this device
	 * @param needle 	the value to search
	 * @param eps  		a tolerance around the returned value
	 * @param bound    	the search boundaries [low, upp] of func
	 * @param func		a monotonic increasing function to search in
	 * @param args		a list of func simple typed arguments
	 * @param fields	a list of func fields arguments 
	 * 
	 * @return          a value x such that |x - func^-1(needle)| <= eps
	 * */
	public static Double binarySearch(ExecutionContext ctx,
			Double needle, Double eps, Tuple bound, JVMEntity func, 
			List<Object> args, Field<?>...fields) 
		throws NoSuchMethodException, IllegalAccessException, 
			   InvocationTargetException {
		
		BinarySearch.uid = ctx.getDeviceUID();
		
		Object  [] funcArgs = ObjectArrays
				.concat(args.toArray(), fields, Object.class);
		Class<?>[] argT = ObjectArrays.concat(
			Double.class, Stream.of(funcArgs)
				.map(Object::getClass).toArray(Class<?>[]::new));
		Method method = func.getType().getMethod(func.getMemberName(), argT);
		Double lowerBound = (Double) bound.get(0),
			   upperBound = (Double) bound.get(1), mid, res;
		
		while (upperBound - lowerBound > eps) {
			mid = (lowerBound + upperBound) / 2;
			res = (Double) method
				.invoke(null, ObjectArrays.concat(mid, funcArgs));
			if (res < needle) lowerBound = mid; 
			else upperBound = mid;
		}
		return upperBound;
	}
}
