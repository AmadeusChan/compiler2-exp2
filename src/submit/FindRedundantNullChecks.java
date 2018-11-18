package submit;

import java.util.*;

import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Operand.RegisterOperand;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadIterator;
import joeq.Compiler.Quad.QuadVisitor;
import joeq.Compiler.Quad.Operator;

import java.util.Set;
import java.util.TreeSet;

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

		public void preprocess(ControlFlowGraph cfg) {
			// TODO
		}

		public void postprocess(ControlFlowGraph cfg) {
			// TODO
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
				set = new TreeSet<String>();
			}

			public void setToTop() {
				set = new TreeSet<String>(universalSet);
			}

			public void setToBottom() {
				set = new TreeSet<String>();
			}

			public void meetWith(Flow.DataflowObject o) {
				ArrayList<String> removeList = new ArrayList<String>();
				VarSet a = (VarSet) o;
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
				return set.hashCode;
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
		};
	}

	public static void main(String[] _args) {
		List<String> args = new ArrayList<String>(Arrays.asList(_args));
		boolean extra = args.contains("-e");
		if (extra)
		args.remove("-e");
		// TODO: Fill in this
	}
}
