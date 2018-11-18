package submit;

import java.util.*;

import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadIterator;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.Operator;
import joeq.Class.*;
import joeq.Main.*;

import java.util.Set;
import java.util.TreeSet;

import flow.Flow;
import flow.FlowSolver;

// Redundant Null Check data-flow analysis
// Using data-flow analysis to find in the set of varaibles which are already checked in the entry and exit of every quad
// flow-direction: forward
// meet operator: intersection
// domain: sets of variables
// top: the including all variables
// buttom: empty set
// transfer function: 
// 	deleted all variables defined in this quad
// 	if it is a NULLCHECK quad, add the varaible which is checked

public class FindRedundantNullChecks {

    /**
     * Main method of FindRedundantNullChecks.
     * This method should print out a list of quad ids of redundant null checks for each function.
     * The format should be "method_name id0 id1 id2", integers for each id separated by spaces.
     *
     * @param args an array of class names. If "-e" presented, do extra analysing.
     */

	// if the quad check a safe variable, this null-check is redundant
	public static class SafeVariableAnalysis implements Flow.Analysis {
		private VarSet[] in, out;
		private VarSet entry, exit;
		private TransferFunction transferfn = new TransferFunction();

		private boolean printMsg = true;

		public void disablePrintMsg() {
			printMsg = false;
		}

		public boolean checkWhetherRedundant(Quad q) {
			if (q.getOperator() instanceof Operator.NullCheck) {
				for (RegisterOperand use: q.getUsedRegisters()) {
					if (in[q.getID()].hasVar(use.getRegister().toString()) == false) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		public void preprocess(ControlFlowGraph cfg) {
			//System.out.println(cfg.getMethod().getName().toString());

			QuadIterator qit = new QuadIterator(cfg);
			int max = 0;
			while (qit.hasNext()) {
				int x = qit.next().getID();
				if (x > max) max = x;
			}
			max += 1;
			in = new VarSet[max];
			out = new VarSet[max];
			qit = new QuadIterator(cfg);

			Set<String> s = new TreeSet<String>();
			VarSet.universalSet = s;

			int numargs = cfg.getMethod().getParamTypes().length;
			for (int i = 0; i < numargs; ++ i) {
				s.add("R" + i);
			}

			while (qit.hasNext()) {
				Quad q = qit.next();
				for (RegisterOperand def: q.getDefinedRegisters()) {
					s.add(def.getRegister().toString());
				}
				for (RegisterOperand use: q.getUsedRegisters()) {
					s.add(use.getRegister().toString());
				}
			}

			entry = new VarSet();
			entry.setToBottom();
			exit = new VarSet();
			transferfn.val = new VarSet();
			for (int i = 0; i < in.length; ++ i) {
				in[i] = new VarSet();
				out[i] = new VarSet();
				//in[i].setToTop();
				//out[i].setToTop();
			}

		}

		public void postprocess(ControlFlowGraph cfg) {
			//System.out.println("entry: " + entry.toString());
			//QuadIterator qit = new QuadIterator(cfg);
			//while (qit.hasNext()) {
			//	Quad q = qit.next();
			//	if (q.getOperator() instanceof Operator.NullCheck) {
			//		System.out.println(q.getID() + " in: " + in[q.getID()].toString());
			//		System.out.println(q.getID() + " out: " + out[q.getID()].toString());
			//	}
			//}
			//System.out.println("exit: " + exit.toString());

			if (printMsg) {
				System.out.print(cfg.getMethod().getName().toString());
				QuadIterator qit = new QuadIterator(cfg);
				ArrayList<Integer> qid_list = new ArrayList<Integer>();
				while (qit.hasNext()) {
					Quad q = qit.next();
					if (q.getOperator() instanceof Operator.NullCheck) {
						boolean is_redundant = true;
						for (RegisterOperand use: q.getUsedRegisters()) {
							if (in[q.getID()].hasVar(use.getRegister().toString()) == false) {
								is_redundant = false;
								break;
							}
						}
						if (is_redundant) qid_list.add(q.getID());
					}
				}
				Collections.sort(qid_list);
				for (int qid: qid_list) System.out.print(" " + qid);
				System.out.println("");
			} else {
				QuadIterator qit = new QuadIterator(cfg);
				while (qit.hasNext()) {
					Quad q = qit.next();
					if (checkWhetherRedundant(q)) {
						//System.out.println("Remove " + q.getID());
						qit.remove();
					}
				}
			}
		}

		public boolean isForward() {
			return true;
		}

		public Flow.DataflowObject getEntry() {
			Flow.DataflowObject result = newTempVar();
			result.copy(entry);
			return result;
		}
		
		public void setEntry(Flow.DataflowObject value) {
			entry.copy(value);
		}
		
		public Flow.DataflowObject getExit() {
			Flow.DataflowObject result = newTempVar();
			result.copy(exit);
			return result;
		}
		
		public void setExit(Flow.DataflowObject value) {
			exit.copy(value);
		}
		
		public Flow.DataflowObject getIn(Quad q) {
			Flow.DataflowObject result = newTempVar();
			result.copy(in[q.getID()]);
			return result;
		}
		
		public Flow.DataflowObject getOut(Quad q) {
			Flow.DataflowObject result = newTempVar();
			result.copy(out[q.getID()]);
			return result;
		}
		
		public void setIn(Quad q, Flow.DataflowObject value) {
			in[q.getID()].copy(value);
		}
		
		public void setOut(Quad q, Flow.DataflowObject value) {
			out[q.getID()].copy(value);
		}
		
		public Flow.DataflowObject newTempVar() {
			return new VarSet();
		}
		
		public void processQuad(Quad q) {
			transferfn.val.copy(in[q.getID()]);
			transferfn.visitQuad(q);
			out[q.getID()].copy(transferfn.val);
		}

		public static class VarSet implements Flow.DataflowObject {
			public static Set<String> universalSet;
			private Set<String> set;

			public VarSet() {
				set = new TreeSet<String>(universalSet);
			}

			public void setToTop() {
				set = new TreeSet<String>(universalSet);
			}

			public void setToBottom() {
				set = new TreeSet<String>();
			}

			public void meetWith(Flow.DataflowObject o) {
				VarSet a = (VarSet) o;
				//set.addAll(a.set);
				ArrayList<String> removeList = new ArrayList<String>();
				for (String v: set) {
					if (! a.set.contains(v)) {
						removeList.add(v);
					}
				}
				set.removeAll(removeList);
			}

			public void copy(Flow.DataflowObject o) {
				VarSet a = (VarSet) o;
				set = new TreeSet<String>(a.set);
			}

			@Override
			public boolean equals(Object o) {
				if (o instanceof VarSet) {
					VarSet a = (VarSet) o;
					return set.equals(a.set);
				}
				return false;
			}

			@Override
			public int hashCode() {
				return set.hashCode();
			}

			@Override
			public String toString() {
				return set.toString();
			}

			public void genVar(String v) {
				set.add(v);
			}

			public void killVar(String v) {
				set.remove(v);
			}

			public boolean hasVar(String v) {
				return set.contains(v);
			}
		}

		public static class TransferFunction extends QuadVisitor.EmptyVisitor {
			VarSet val;
		
			@Override
			public void visitQuad(Quad q) {
				if (q.getOperator() instanceof Operator.NullCheck) {
					for (RegisterOperand use: q.getUsedRegisters()) {
						val.genVar(use.getRegister().toString());
					}
				} else {
					for (RegisterOperand def: q.getDefinedRegisters()) {
						val.killVar(def.getRegister().toString());
					}
				}
			}
		}
	}

	public static void main(String[] _args) {
		List<String> args = new ArrayList<String>(Arrays.asList(_args));
		boolean extra = args.contains("-e");
		if (extra) {
			args.remove("-e");
		}
		
		jq_Class[] classes = new jq_Class[args.size()];
		for (int i = 0; i < classes.length; ++ i) {
			classes[i] = (jq_Class) Helper.load(args.get(i));
		}

		Flow.Solver solver = new FlowSolver();
		Flow.Analysis analysis = new SafeVariableAnalysis();
		solver.registerAnalysis(analysis);

		for (int i = 0; i < classes.length; ++ i) {
			Helper.runPass(classes[i], solver);
		}
	}
}
