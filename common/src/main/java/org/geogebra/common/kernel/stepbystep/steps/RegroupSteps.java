package org.geogebra.common.kernel.stepbystep.steps;

import static org.geogebra.common.kernel.stepbystep.steptree.StepExpression.*;

import java.util.*;

import org.geogebra.common.kernel.stepbystep.StepHelper;
import org.geogebra.common.kernel.stepbystep.solution.SolutionBuilder;
import org.geogebra.common.kernel.stepbystep.solution.SolutionLine;
import org.geogebra.common.kernel.stepbystep.solution.SolutionStepType;
import org.geogebra.common.kernel.stepbystep.steptree.StepConstant;
import org.geogebra.common.kernel.stepbystep.steptree.StepExpression;
import org.geogebra.common.kernel.stepbystep.steptree.StepNode;
import org.geogebra.common.kernel.stepbystep.steptree.StepOperation;
import org.geogebra.common.plugin.Operation;

public enum RegroupSteps implements SimplificationStepGenerator {

	DECIMAL_SIMPLIFY_ROOTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation root = (StepOperation) sn;
				StepExpression underRoot = root.getOperand(0);
				StepExpression coefficient = underRoot.getIntegerCoefficient();
				StepExpression remainder = underRoot.getNonInteger();

				if (!isZero(coefficient) && !isZero(remainder)) {
					StepExpression firstPart = root(coefficient, root.getOperand(1));
					StepExpression secondPart = root(remainder, root.getOperand(1));
					StepExpression result = multiply(firstPart, secondPart);

					root.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.SPLIT_ROOTS, tracker.incColorTracker());
					return result;
				}

				if (!isZero(coefficient)) {
					StepExpression result = StepConstant.create(root.getValue());

					root.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.EVALUATE_ROOT, tracker.incColorTracker());
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DECIMAL_SIMPLIFY_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation fraction = (StepOperation) sn;
				StepExpression numerator = fraction.getOperand(0);
				StepExpression denominator = fraction.getOperand(1);
				StepExpression numeratorCoefficient = numerator.getIntegerCoefficient();
				StepExpression denominatorCoefficient = denominator.getIntegerCoefficient();
				StepExpression numeratorRemainder = numerator.getNonInteger();
				StepExpression denominatorRemainder = denominator.getNonInteger();

				if (!isOne(denominatorCoefficient) && (!isOne(numeratorRemainder) || !isOne(denominatorRemainder))) {
					StepExpression firstPart = divide(numeratorCoefficient, denominatorCoefficient);
					StepExpression secondPart = divide(numeratorRemainder, denominatorRemainder);
					StepExpression result = multiply(firstPart, secondPart);

					fraction.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.SPLIT_FRACTIONS, tracker.incColorTracker());
					return result;
				}

				if (!isOne(denominatorCoefficient)) {
					StepExpression result = StepConstant.create(fraction.getValue());

					fraction.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.EVALUATE_FRACTION, tracker.incColorTracker());
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	CONVERT_DECIMAL_TO_FRACTION_SUBSTEP {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn instanceof StepExpression) {
				StepExpression se = (StepExpression) sn;

				if (se.nonSpecialConstant() && !se.isInteger()) {
					int decimalLength = Double.toString(se.getValue()).split("\\.")[1].length();

					long numerator = (long) (se.getValue() * Math.pow(10, decimalLength));
					long denominator = (long) Math.pow(10, decimalLength);

					StepExpression result = divide(numerator, denominator);

					se.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.REWRITE_DECIMAL_AS_COMMON_FRACTION, tracker.incColorTracker());
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	CONVERT_DECIMAL_TO_FRACTION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] strategy = new SimplificationStepGenerator[] {
					CONVERT_DECIMAL_TO_FRACTION_SUBSTEP,
					CANCEL_INTEGER_FRACTION
			};

			return StepStrategies.implementGroup(sn, SolutionStepType.CONVERT_DECIMALS, strategy, sb, tracker);
		}
	},

	ELIMINATE_OPPOSITES {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.PLUS)) {
				StepOperation so = (StepOperation) sn;

				StepOperation newSum = new StepOperation(Operation.PLUS);

				boolean[] found = new boolean[so.noOfOperands()];
				for (int i = 0; i < so.noOfOperands(); i++) {
					if (so.getOperand(i).isOperation(Operation.MINUS)
							&& ((StepOperation) so.getOperand(i)).getOperand(0).isOperation(Operation.PLUS)) {
						StepOperation innerSum = (StepOperation) ((StepOperation) so.getOperand(i)).getOperand(0);

						for (int j = 0; j < so.noOfOperands() - innerSum.noOfOperands(); j++) {
							boolean foundSum = true;
							for (int k = 0; foundSum && k < innerSum.noOfOperands(); k++) {
								if (!so.getOperand(j + k).equals(innerSum.getOperand(k))) {
									foundSum = false;
								}
							}

							if (foundSum) {
								found[i] = true;
								so.getOperand(i).setColor(tracker.getColorTracker());

								for (int k = 0; k < innerSum.noOfOperands(); k++) {
									found[j + k] = true;
									so.getOperand(j + k).setColor(tracker.getColorTracker());
								}
								sb.add(SolutionStepType.ELIMINATE_OPPOSITES, tracker.incColorTracker());
								break;
							}
						}
					}

					for (int j = i + 1; !found[i] && j < so.noOfOperands(); j++) {
						if (so.getOperand(i).equals(so.getOperand(j).negate())
								|| so.getOperand(j).equals(so.getOperand(i).negate())) {
							so.getOperand(i).setColor(tracker.getColorTracker());
							so.getOperand(j).setColor(tracker.getColorTracker());
							sb.add(SolutionStepType.ELIMINATE_OPPOSITES, tracker.incColorTracker());
							found[i] = true;
							found[j] = true;
						}
					}
				}

				for (int i = 0; i < so.noOfOperands(); i++) {
					if (!found[i]) {
						newSum.addOperand(so.getOperand(i));
					}
				}

				if (newSum.noOfOperands() == 0) {
					return StepConstant.create(0);
				}

				if (newSum.noOfOperands() == 1) {
					return newSum.getOperand(0);
				}

				if (tracker.wasChanged()) {
					return newSum;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	EXPAND_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				int rootCount = 0;
				long commonRoot = 1;
				for (StepExpression operand : so) {
					if (operand.isOperation(Operation.NROOT)) {
						double currentRoot = ((StepOperation) operand).getOperand(1).getValue();
						if (closeToAnInteger(currentRoot)) {
							rootCount++;
							commonRoot = lcm(commonRoot, Math.round(currentRoot));
						}
					}
				}

				if (rootCount > 1) {
					StepExpression newProduct = null;
					for (StepExpression operand : so) {
						if (operand.isOperation(Operation.NROOT)) {
							double currentRoot = ((StepOperation) operand).getOperand(1).getValue();
							if (closeToAnInteger(currentRoot) && !isEqual(commonRoot, currentRoot)) {
								StepExpression argument = ((StepOperation) operand).getOperand(0);

								StepExpression result = root(power(argument, commonRoot / currentRoot), commonRoot);

								operand.setColor(tracker.getColorTracker());
								result.setColor(tracker.getColorTracker());

								sb.add(SolutionStepType.EXPAND_ROOT, tracker.incColorTracker());

								newProduct = multiply(newProduct, result);
							} else {
								newProduct = multiply(newProduct, operand);
							}
						} else {
							newProduct = multiply(newProduct, operand);
						}
					}

					return newProduct;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	COMMON_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				StepExpression newProduct = null;
				StepExpression underRoot = null;

				int rootCount = 0;
				double commonRoot = 0;
				for (StepExpression operand : so) {
					if (operand.isOperation(Operation.NROOT)) {
						double currentRoot = ((StepOperation) operand).getOperand(1).getValue();
						if (isEqual(commonRoot, 0) || isEqual(commonRoot, currentRoot)) {
							commonRoot = currentRoot;
							underRoot = multiply(underRoot, (((StepOperation) operand).getOperand(0)));
							rootCount++;
						} else {
							newProduct = multiply(newProduct, operand);
						}
					} else {
						newProduct = multiply(newProduct, operand);
					}
				}

				if (rootCount > 1) {
					for (StepExpression operand : so) {
						if (operand.isOperation(Operation.NROOT)) {
							double currentRoot = ((StepOperation) operand).getOperand(1).getValue();
							if (isEqual(commonRoot, currentRoot)) {
								operand.setColor(tracker.getColorTracker());
							}
						}
					}

					StepExpression result = root(underRoot, commonRoot);
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.PRODUCT_OF_ROOTS, tracker.incColorTracker());

					return multiply(newProduct, result);
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SQUARE_ROOT_MULTIPLIED_BY_ITSELF {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				StepExpression newProduct = null;

				boolean[] found = new boolean[so.noOfOperands()];
				for (int i = 0; i < so.noOfOperands(); i++) {
					if (so.getOperand(i).isSquareRoot()) {
						for (int j = i + 1; j < so.noOfOperands(); j++) {
							if (so.getOperand(i).equals(so.getOperand(j))) {
								StepExpression result = ((StepOperation) so.getOperand(i)).getOperand(0).deepCopy();

								found[i] = found[j] = true;

								so.getOperand(i).setColor(tracker.getColorTracker());
								so.getOperand(j).setColor(tracker.getColorTracker());
								result.setColor(tracker.getColorTracker());

								sb.add(SolutionStepType.SQUARE_ROOT_MULTIPLIED_BY_ITSELF, tracker.incColorTracker());

								newProduct = multiply(newProduct, result);
							}
						}
					}

					if (!found[i]) {
						newProduct = multiply(newProduct, so.getOperand(i));
					}
				}

				return newProduct;
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DOUBLE_MINUS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MINUS)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isNegative()) {
					StepExpression result = so.getOperand(0).negate();
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DOUBLE_MINUS, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DISTRIBUTE_POWER_OVER_FRACION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isOperation(Operation.DIVIDE)) {
					StepExpression exponent = so.getOperand(1);
					StepOperation fraction = (StepOperation) so.getOperand(0);

					StepExpression numerator = power(fraction.getOperand(0), exponent);
					StepExpression denominator = power(fraction.getOperand(1), exponent);

					StepExpression result = divide(numerator, denominator);
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DISTRIBUTE_POWER_FRAC, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DISTRIBUTE_ROOT_OVER_FRACTION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isOperation(Operation.DIVIDE)) {
					StepExpression exponent = so.getOperand(1);
					StepOperation fraction = (StepOperation) so.getOperand(0);

					StepExpression numerator = root(fraction.getOperand(0), exponent);
					StepExpression denominator = root(fraction.getOperand(1), exponent);

					StepExpression result = divide(numerator, denominator);
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DISTRIBUTE_ROOT_FRAC, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	RATIONALIZE_SIMPLE_DENOMINATOR {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				List<StepOperation> irrationals = new ArrayList<>();

				if (so.getOperand(1).isOperation(Operation.NROOT)) {
					irrationals.add((StepOperation) so.getOperand(1));
				}

				if (so.getOperand(1).isOperation(Operation.MULTIPLY)) {
					for (StepExpression operand : (StepOperation) so.getOperand(1)) {
						if (operand.isOperation(Operation.NROOT)) {
							irrationals.add((StepOperation) operand);
						}
					}
				}

				StepExpression toMultiply = null;
				for (StepOperation irrational : irrationals) {
					StepExpression root = irrational.getOperand(1);
					StepExpression argument = irrational.getOperand(0);

					StepExpression power = null;
					if (argument.isOperation(Operation.POWER)) {
						power = ((StepOperation) argument).getOperand(1);
						argument = ((StepOperation) argument).getOperand(0);
					}

					if ((power == null || power.isInteger()) && root.isInteger()) {
						double powerVal = power == null ? 1 : power.getValue();
						double rootVal = root.getValue();
						double newPower = rootVal - (powerVal % rootVal);

						toMultiply = multiply(toMultiply, root(nonTrivialPower(argument, newPower), root));
					}
				}

				if (toMultiply != null) {
					toMultiply.setColor(tracker.incColorTracker());

					StepExpression numerator = nonTrivialProduct(so.getOperand(0), toMultiply);
					StepExpression denominator = multiply(so.getOperand(1), toMultiply);

					StepExpression result = divide(numerator, denominator);
					sb.add(SolutionStepType.MULTIPLY_NUM_DENOM, toMultiply);

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	RATIONALIZE_COMPLEX_DENOMINATOR {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(1).isOperation(Operation.PLUS)) {
					StepOperation sum = (StepOperation) so.getOperand(1);

					if (sum.noOfOperands() == 2
							&& (sum.getOperand(0).containsSquareRoot() || sum.getOperand(1).containsSquareRoot())) {
						StepExpression toMultiply;

						if (sum.getOperand(0).isNegative()) {
							toMultiply = add(sum.getOperand(0).negate(), sum.getOperand(1));
						} else {
							toMultiply = add(sum.getOperand(0), sum.getOperand(1).negate());
						}

						toMultiply.setColor(tracker.incColorTracker());

						StepExpression numerator = nonTrivialProduct(so.getOperand(0), toMultiply);
						StepExpression denominator = multiply(so.getOperand(1), toMultiply);

						StepExpression result = divide(numerator, denominator);
						sb.add(SolutionStepType.MULTIPLY_NUM_DENOM, toMultiply);

						tracker.addMark(denominator, RegroupTracker.MarkType.EXPAND);
						return result;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DISTRIBUTE_MINUS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MINUS)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isOperation(Operation.PLUS)) {
					StepExpression result = null;
					for (StepExpression operand : (StepOperation) so.getOperand(0)) {
						result = add(result, operand.negate());
					}

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DISTRIBUTE_MINUS, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REWRITE_INTEGER_UNDER_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				StepExpression underRoot = so.getOperand(0);
				StepExpression root = so.getOperand(1);

				List<StepExpression> irrationals = new ArrayList<>();

				if (underRoot.isOperation(Operation.MULTIPLY)) {
					for (StepExpression operand : (StepOperation) underRoot) {
						irrationals.add(operand);
					}
				} else {
					irrationals.add(underRoot);
				}

				long rootVal = Math.round(root.getValue());

				StepExpression newRoot = null;
				for (StepExpression irrational : irrationals) {
					if (irrational.isInteger() && !irrational.isNegative()) {
						long power = getIntegerPower(Math.round(irrational.getValue()));
						long gcd = gcd(rootVal, power);

						long newPower = 0;
						if (gcd > 1 && irrationals.size() == 1) {
							newPower = gcd;
						} else if (power >= rootVal) {
							newPower = power;
						}

						if (newPower != 0) {
							StepExpression newValue = power(
									StepConstant.create(Math.pow(irrational.getValue(), ((double) 1) / newPower)), newPower);

							irrational.setColor(tracker.getColorTracker());
							newValue.setColor(tracker.incColorTracker());

							newRoot = multiply(newRoot, newValue);

							sb.add(SolutionStepType.REWRITE_AS, irrational, newValue);
							continue;
						}

						long newCoefficient = largestNthPower(irrational, rootVal);

						if (newCoefficient != 1) {
							StepExpression firstPart = StepConstant.create(newCoefficient);
							StepExpression secondPart = StepConstant.create(irrational.getValue() / newCoefficient);

							firstPart.setColor(tracker.getColorTracker());
							secondPart.setColor(tracker.incColorTracker());

							newRoot = multiply(newRoot, multiply(firstPart, secondPart));

							sb.add(SolutionStepType.REWRITE_AS, irrational, multiply(firstPart, secondPart));
							continue;
						}
					}

					newRoot = multiply(newRoot, irrational);
				}

				newRoot = root(newRoot, root);

				if (!so.equals(newRoot)) {
					return newRoot;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REWRITE_ROOT_UNDER_POWER {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER) && ((StepOperation) sn).getOperand(0).isOperation(Operation.NROOT)) {
				StepOperation power = (StepOperation) sn;
				StepOperation root = (StepOperation) power.getOperand(0);

				StepExpression quotient = power.getOperand(1).quotient(root.getOperand(1));
				StepExpression remainder = power.getOperand(1).remainder(root.getOperand(1));

				if (!isZero(quotient) && !isZero(remainder)) {
					StepExpression newPower = nonTrivialProduct(quotient, root.getOperand(1));
					StepExpression result = multiply(nonTrivialPower(root, newPower), nonTrivialPower(root, remainder));

					sn.setColor(tracker.getColorTracker());
					result.setColor(tracker.incColorTracker());

					sb.add(SolutionStepType.REWRITE_AS, sn, result);
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REWRITE_POWER_UNDER_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				StepExpression underRoot = so.getOperand(0);
				StepExpression root = so.getOperand(1);

				List<StepExpression> irrationals = new ArrayList<>();

				if (underRoot.isOperation(Operation.MULTIPLY)) {
					for (StepExpression operand : (StepOperation) underRoot) {
						irrationals.add(operand);
					}
				} else {
					irrationals.add(underRoot);
				}

				StepExpression newRoot = null;
				for (StepExpression irrational : irrationals) {
					if (irrational.isOperation(Operation.POWER)) {
						StepExpression underPower = ((StepOperation) irrational).getOperand(0);
						StepExpression quotient = irrational.getPower().quotient(root);
						StepExpression remainder = irrational.getPower().remainder(root);

						if (!isZero(quotient) && !isZero(remainder)) {
							StepExpression firstPart = power(underPower, nonTrivialProduct(root, quotient));
							StepExpression secondPart = nonTrivialPower(underPower, remainder);

							irrational.setColor(tracker.getColorTracker());
							firstPart.setColor(tracker.getColorTracker());
							secondPart.setColor(tracker.incColorTracker());

							newRoot = multiply(newRoot, multiply(firstPart, secondPart));

							sb.add(SolutionStepType.SPLIT_POWERS, irrational, multiply(firstPart, secondPart));
							continue;
						}
					}

					newRoot = multiply(newRoot, irrational);
				}

				newRoot = root(newRoot, root);

				if (!so.equals(newRoot)) {
					return newRoot;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SPLIT_ROOTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				StepExpression underRoot = so.getOperand(0);
				StepExpression root = so.getOperand(1);

				List<StepExpression> irrationals = new ArrayList<>();

				if (underRoot.isOperation(Operation.MULTIPLY)) {
					for (StepExpression operand : (StepOperation) underRoot) {
						irrationals.add(operand);
					}
				} else {
					irrationals.add(underRoot);
				}

				StepExpression newRoot = null;
				StepExpression separateRoots = null;

				for (StepExpression irrational : irrationals) {
					if (irrational.isOperation(Operation.POWER)) {
						StepExpression remainder = irrational.getPower().remainder(root);

						if (isZero(remainder)) {
							separateRoots = multiply(separateRoots, root(irrational, root));
							continue;
						}
					}

					newRoot = multiply(newRoot, irrational);
				}

				if (newRoot != null & separateRoots != null) {
					StepExpression result = multiply(separateRoots, root(newRoot, root));

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.SPLIT_ROOTS, tracker.incColorTracker());
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REGROUP_SUMS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.PLUS)) {
				int colorsAtStart = tracker.getColorTracker();

				StepNode tempResult = regroupSums((StepOperation) sn, sb, tracker, false);
				if (colorsAtStart != tracker.getColorTracker()) {
					return tempResult;
				}

				tempResult = regroupSums((StepOperation) sn, sb, tracker, true);
				if (colorsAtStart != tracker.getColorTracker()) {
					return tempResult;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}

		private StepNode regroupSums(StepOperation so, SolutionBuilder sb, RegroupTracker tracker, boolean integer) {
			StepExpression[] coefficients = new StepExpression[so.noOfOperands()];
			StepExpression[] variables = new StepExpression[so.noOfOperands()];
			for (int i = 0; i < so.noOfOperands(); i++) {
				if (integer) {
					coefficients[i] = so.getOperand(i).getIntegerCoefficient();
					variables[i] = so.getOperand(i).getNonInteger();
				} else {
					coefficients[i] = so.getOperand(i).getCoefficient();
					variables[i] = so.getOperand(i).getVariable();
				}

				if (coefficients[i] == null) {
					coefficients[i] = StepConstant.create(1);
				}
				if (variables[i] == null) {
					variables[i] = StepConstant.create(1);
				}
			}

			List<StepExpression> constantList = new ArrayList<>();
			double constantSum = 0;
			for (int i = 0; i < so.noOfOperands(); i++) {
				if ((coefficients[i].nonSpecialConstant() ||
						coefficients[i].specialConstant() && tracker.isDecimalSimplify()) && isOne(variables[i])) {
					constantList.add(coefficients[i]);
					constantSum += coefficients[i].getValue();
					coefficients[i] = null;
				}
			}

			for (int i = 0; i < so.noOfOperands(); i++) {
				if ((integer || !variables[i].isConstant()) && !isZero(coefficients[i])) {
					boolean foundCommon = false;
					for (int j = i + 1; j < so.noOfOperands(); j++) {
						if (!isZero(coefficients[j]) && !isOne(variables[i]) && variables[i].equals(variables[j])) {
							foundCommon = true;
							so.getOperand(j).setColor(tracker.getColorTracker());
							coefficients[i] = add(coefficients[i], coefficients[j]);
							coefficients[j] = null;
						}
					}
					if (foundCommon) {
						so.getOperand(i).setColor(tracker.getColorTracker());
						coefficients[i].setColor(tracker.getColorTracker());
						variables[i].setColor(tracker.getColorTracker());
						sb.add(SolutionStepType.COLLECT_LIKE_TERMS, variables[i]);
						tracker.incColorTracker();
					}
				}
			}

			StepOperation newSum = new StepOperation(Operation.PLUS);

			for (int i = 0; i < so.noOfOperands(); i++) {
				if (!isZero(coefficients[i]) && !isZero(variables[i])) {
					newSum.addOperand(simplifiedProduct(coefficients[i], variables[i]));
				}
			}

			StepExpression newConstants = StepConstant.create(constantSum);
			if (constantList.size() > 1) {
				for (StepExpression constant: constantList) {
					constant.setColor(tracker.getColorTracker());
				}
				sb.add(SolutionStepType.ADD_CONSTANTS, tracker.getColorTracker());
				newConstants.setColor(tracker.getColorTracker());
				tracker.incColorTracker();
			}

			if (isEqual(constantSum, 0) && constantList.size() == 1) {
				constantList.get(0).setColor(tracker.getColorTracker());
				sb.add(SolutionStepType.ZERO_IN_ADDITION, tracker.incColorTracker());
			}

			if (!isEqual(constantSum, 0)) {
				newSum.addOperand(newConstants);
			}

			if (newSum.noOfOperands() == 0) {
				return StepConstant.create(0);
			} else if (newSum.noOfOperands() == 1) {
				return newSum.getOperand(0);
			}

			return newSum;
		}
	},

	TRIVIAL_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				if (isEqual(so.getOperand(0), 0)) {
					StepExpression result = StepConstant.create(0);
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.ZERO_DIVIDED, tracker.incColorTracker());
					return result;
				}

				if (isEqual(so.getOperand(1), 0)) {
					StepExpression result = StepConstant.UNDEFINED;
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.DIVIDED_BY_ZERO, tracker.incColorTracker());
					return result;
				}

				if (isEqual(so.getOperand(1), 1)) {
					so.getOperand(1).setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DIVIDE_BY_ONE, tracker.incColorTracker());
					return so.getOperand(0).deepCopy();
				}

				if (isEqual(so.getOperand(1), -1)) {
					so.getOperand(1).setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.DIVIDE_BY_NEGATVE_ONE, tracker.incColorTracker());
					return minus(so.getOperand(0).deepCopy());
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	FACTOR_MINUS_FROM_SUMS {
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				List<StepExpression> basesNumerator = new ArrayList<>();
				List<StepExpression> exponentsNumerator = new ArrayList<>();
				List<StepExpression> basesDenominator = new ArrayList<>();
				List<StepExpression> exponentsDenominator = new ArrayList<>();

				so.getOperand(0).getBasesAndExponents(basesNumerator, exponentsNumerator);
				so.getOperand(1).getBasesAndExponents(basesDenominator, exponentsDenominator);

				for (int i = 0; i < basesNumerator.size(); i++) {
					if (!basesNumerator.get(i).isSum()) {
						continue;
					}

					for (int j = 0; j < basesDenominator.size(); j++) {
						if (basesDenominator.get(j).isSum()) {
							StepOperation negated = new StepOperation(Operation.PLUS);
							for (StepExpression operand : (StepOperation) basesNumerator.get(i)) {
								negated.addOperand(operand.negate());
							}

							if (negated.equals(basesDenominator.get(j))) {
								basesNumerator.get(i).setColor(tracker.getColorTracker());
								basesDenominator.get(j).setColor(tracker.getColorTracker());
								negated.setColor(tracker.incColorTracker());

								basesNumerator.set(i, negated.negate());

								sb.add(SolutionStepType.FACTOR_MINUS);
							}
						}
					}
				}

				StepExpression numerator = null;
				for (int i = 0; i < basesNumerator.size(); i++) {
					numerator = multiply(numerator,
							nonTrivialPower(basesNumerator.get(i), exponentsNumerator.get(i)));
				}

				return divide(numerator, so.getOperand(1));
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	FACTOR_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				SolutionBuilder temp = new SolutionBuilder();

				StepOperation factored = (StepOperation)
						FactorSteps.SIMPLE_FACTOR.apply(so.deepCopy(), temp, new RegroupTracker());

				if (!isOne(StepHelper.weakGCD(factored.getOperand(0), factored.getOperand(1)))
						&& !so.equals(factored)) {
					sb.addGroup(new SolutionLine(SolutionStepType.FACTOR, sn), temp, factored);

					tracker.incColorTracker();
					return factored;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	CANCEL_INTEGER_FRACTION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				List<StepExpression> basesNumerator = new ArrayList<>();
				List<StepExpression> exponentsNumerator = new ArrayList<>();
				List<StepExpression> basesDenominator = new ArrayList<>();
				List<StepExpression> exponentsDenominator = new ArrayList<>();

				so.getOperand(0).getBasesAndExponents(basesNumerator, exponentsNumerator);
				so.getOperand(1).getBasesAndExponents(basesDenominator, exponentsDenominator);

				int colorsAtStart = tracker.getColorTracker();

				for (int i = 0; i < basesNumerator.size(); i++) {
					for (int j = 0; j < basesDenominator.size(); j++) {
						if (isEqual(exponentsNumerator.get(i), 1) && isEqual(exponentsDenominator.get(j), 1)
								&& basesNumerator.get(i).isInteger() && basesDenominator.get(j).isInteger()) {
							long gcd = gcd(basesNumerator.get(i), basesDenominator.get(j));

							if (gcd > 1) {
								basesNumerator.get(i).setColor(tracker.getColorTracker());
								basesDenominator.get(j).setColor(tracker.getColorTracker());

								basesNumerator.set(i, StepConstant.create(basesNumerator.get(i).getValue() / gcd));
								basesDenominator.set(j, StepConstant.create(basesDenominator.get(j).getValue() / gcd));

								basesNumerator.get(i).setColor(tracker.getColorTracker());
								basesDenominator.get(j).setColor(tracker.getColorTracker());

								StepExpression toCancel = StepConstant.create(gcd);
								toCancel.setColor(tracker.incColorTracker());
								sb.add(SolutionStepType.CANCEL_FRACTION, toCancel);

								break;
							}
						}
					}
				}

				if (tracker.getColorTracker() == colorsAtStart) {
					return StepStrategies.iterateThrough(this, sn, sb, tracker);
				}

				return makeFraction(basesNumerator, exponentsNumerator, basesDenominator, exponentsDenominator);
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	CANCEL_FRACTION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				List<StepExpression> basesNumerator = new ArrayList<>();
				List<StepExpression> exponentsNumerator = new ArrayList<>();
				List<StepExpression> basesDenominator = new ArrayList<>();
				List<StepExpression> exponentsDenominator = new ArrayList<>();

				so.getOperand(0).getBasesAndExponents(basesNumerator, exponentsNumerator);
				so.getOperand(1).getBasesAndExponents(basesDenominator, exponentsDenominator);

				int colorsAtStart = tracker.getColorTracker();

				for (int i = 0; i < basesNumerator.size(); i++) {
					if (isOne(basesNumerator.get(i))) {
						continue;
					}

					for (int j = 0; j < basesDenominator.size(); j++) {
						StepExpression common = exponentsNumerator.get(i).getCommon(exponentsDenominator.get(j));

						if (basesNumerator.get(i).equals(basesDenominator.get(j)) && !isZero(common)) {
							basesNumerator.get(i).setColor(tracker.getColorTracker());

							basesDenominator.get(j).setColor(tracker.getColorTracker());

							exponentsNumerator.get(i).setColor(tracker.getColorTracker());
							exponentsDenominator.get(j).setColor(tracker.getColorTracker());

							exponentsNumerator.set(i, subtract(exponentsNumerator.get(i), common));
							exponentsDenominator.set(j, subtract(exponentsDenominator.get(j), common));
							exponentsNumerator.set(i, (StepExpression)
									WEAK_REGROUP.apply(exponentsNumerator.get(i),
											new SolutionBuilder(), new RegroupTracker()));
							exponentsDenominator.set(j, (StepExpression)
									WEAK_REGROUP.apply(exponentsDenominator.get(j),
											new SolutionBuilder(), new RegroupTracker()));


							exponentsNumerator.get(i).setColor(tracker.getColorTracker());
							exponentsDenominator.get(j).setColor(tracker.getColorTracker());

							StepExpression toCancel = nonTrivialPower(basesNumerator.get(i), common);
							toCancel.setColor(tracker.incColorTracker());

							sb.add(SolutionStepType.CANCEL_FRACTION, toCancel);
							break;
						}
					}
				}

				if (tracker.getColorTracker() == colorsAtStart) {
					return StepStrategies.iterateThrough(this, sn, sb, tracker);
				}

				return makeFraction(basesNumerator, exponentsNumerator, basesDenominator, exponentsDenominator);
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	COMMON_FRACTION {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				StepExpression numerator = null;
				StepExpression denominator = null;

				for (StepExpression operand : so) {
					numerator = multiply(numerator, operand.getNumerator());
					denominator = multiply(denominator, operand.getDenominator());
				}

				StepExpression result = divide(numerator, denominator);

				if (!so.equals(result)) {
					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.COMMON_FRACTION, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}

	},

	NEGATIVE_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				StepExpression result = null;
				if (so.getOperand(0).isNegative() && so.getOperand(1).isNegative()) {
					result = divide(so.getOperand(0).negate(), so.getOperand(1).negate());
					sb.add(SolutionStepType.NEGATIVE_NUM_AND_DENOM);
				} else if (so.getOperand(0).isNegative()) {
					result = divide(so.getOperand(0).negate(), so.getOperand(1)).negate();
					sb.add(SolutionStepType.NEGATIVE_NUM_OR_DENOM);
				} else if (so.getOperand(1).isNegative()) {
					result = divide(so.getOperand(0), so.getOperand(1).negate()).negate();
					sb.add(SolutionStepType.NEGATIVE_NUM_OR_DENOM);
				}

				if (result != null) {
					sn.setColor(tracker.getColorTracker());
					result.setColor(tracker.incColorTracker());
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	MULTIPLY_NEGATIVES {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				int negativeCount = 0;
				StepExpression result = null;

				for (StepExpression operand : so) {
					if (operand.isNegative()) {
						negativeCount++;
						result = multiply(result, operand.negate());
					} else {
						result = multiply(result, operand);
					}
				}

				if (negativeCount == 0) {
					return StepStrategies.iterateThrough(this, sn, sb, tracker);
				}

				sn.setColor(tracker.getColorTracker());
				result.setColor(tracker.getColorTracker());

				if (negativeCount % 2 == 0) {
					sb.add(SolutionStepType.EVEN_NUMBER_OF_NEGATIVES, tracker.incColorTracker());
					return result;
				}

				sb.add(SolutionStepType.ODD_NUMBER_OF_NEGATIVES, tracker.incColorTracker());
				return minus(result);
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REWRITE_COMPLEX_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.DIVIDE)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isFraction() || so.getOperand(1).isFraction()) {
					StepExpression result = nonTrivialProduct(so.getOperand(0), so.getOperand(1).reciprocate());

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.REWRITE_COMPLEX_FRACTION, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	COLLECT_LIKE_TERMS_PRODUCT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				int colorsAtStart = tracker.getColorTracker();

				StepOperation so = (StepOperation) sn;

				List<StepExpression> bases = new ArrayList<>();
				List<StepExpression> exponents = new ArrayList<>();

				so.getBasesAndExponents(bases, exponents);

				for (int i = 0; i < bases.size(); i++) {
					if (exponents.get(i) == null) {
						continue;
					}

					boolean foundCommon = false;
					for (int j = i + 1; j < bases.size(); j++) {
						if (exponents.get(j) == null) {
							continue;
						}

						if (bases.get(i).equals(bases.get(j))) {
							foundCommon = true;
							bases.get(j).setColor(tracker.getColorTracker());

							exponents.set(i, add(exponents.get(i), exponents.get(j)));
							exponents.set(j, null);
						}
					}

					if (foundCommon) {
						bases.get(i).setColor(tracker.getColorTracker());
						exponents.get(i).setColor(tracker.incColorTracker());

						sb.add(SolutionStepType.REGROUP_PRODUCTS, bases.get(i));
					}
				}

				StepExpression newProduct = null;
				for (int i = 0; i < bases.size(); i++) {
					if (exponents.get(i) != null) {
						newProduct = multiply(newProduct, nonTrivialPower(bases.get(i), exponents.get(i)));
					}
				}

				if (tracker.getColorTracker() > colorsAtStart) {
					return newProduct;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	MULTIPLIED_BY_ZERO {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				for (StepExpression operand : so) {
					if (isZero(operand)) {
						StepExpression result = StepConstant.create(0);

						so.setColor(tracker.getColorTracker());
						result.setColor(tracker.getColorTracker());

						sb.add(SolutionStepType.MULTIPLIED_BY_ZERO, tracker.incColorTracker());
						return result;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	REWRITE_AS_EXPONENTIAL {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				List<StepExpression> bases = new ArrayList<>();
				List<StepExpression> exponents = new ArrayList<>();

				so.getBasesAndExponents(bases, exponents);

				List<StepExpression> basesToConvert = new ArrayList<>();
				for (int i = 0; i < bases.size(); i++) {
					for (int j = i + 1; j < bases.size(); j++) {
						if (bases.get(i).isInteger() && bases.get(j).isInteger() &&
								(!isEqual(exponents.get(i), 1) || !isEqual(exponents.get(j), 1))) {
							long valA = Math.round(bases.get(i).getValue());
							long valB = Math.round(bases.get(j).getValue());

							double baseA = Math.pow(valA, 1.0 / StepNode.getIntegerPower(valA));
							double baseB = Math.pow(valB, 1.0 / StepNode.getIntegerPower(valB));

							if (isEqual(baseA, baseB)) {
								if (!isEqual(bases.get(i), baseA)) {
									basesToConvert.add(bases.get(i));
								}

								if (!isEqual(bases.get(j), baseB)) {
									basesToConvert.add(bases.get(j));
								}
							}
						}
					}
				}

				if (basesToConvert.isEmpty()) {
					return sn;
				}

				for (int i = 0; i < bases.size(); i++) {
					if (basesToConvert.contains(bases.get(i))) {
						long value = Math.round(bases.get(i).getValue());
						double exponent = StepNode.getIntegerPower(value);
						double base = Math.pow(value, 1 / exponent);

						StepExpression result = power(StepConstant.create(base), exponent);

						bases.get(i).setColor(tracker.getColorTracker());
						result.setColor(tracker.incColorTracker());

						sb.add(SolutionStepType.REWRITE_AS, bases.get(i), result);

						bases.set(i, result);
					}
				}

				StepExpression result = null;
				for (int i = 0; i < bases.size(); i++) {
					result = multiply(result, nonTrivialPower(bases.get(i), exponents.get(i)));
				}

				return result;
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	MULTIPLY_CONSTANTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.MULTIPLY)) {
				StepOperation so = (StepOperation) sn;

				List<StepExpression> constantList = new ArrayList<>();
				StepExpression nonConstant = null;

				double constantValue = 1;
				for (StepExpression operand : so) {
					if (operand.nonSpecialConstant()) {
						constantList.add(operand);
						constantValue *= operand.getValue();
					} else {
						nonConstant = multiply(nonConstant, operand);
					}
				}

				if (constantList.size() == 1 && isEqual(constantValue, 1)) {
					constantList.get(0).setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.MULTIPLIED_BY_ONE, tracker.incColorTracker());

					return nonConstant;
				}

				if (constantList.size() < 2) {
					return StepStrategies.iterateThrough(this, sn, sb, tracker);
				}

				/*
				If the expression is under root, then we shouldn't regroup 3*3 to 9, because
				3^2 is probably better. Also, 2*2^3 should be 2^4
				 */
				List<StepExpression> bases = new ArrayList<>();
				List<StepExpression> exponents = new ArrayList<>();
				so.getBasesAndExponents(bases, exponents);

				boolean foundCommon = false;
				for (int i = 0; i < bases.size(); i++) {
					for (int j = i + 1; j < bases.size(); j++) {
						if (bases.get(i).equals(bases.get(j))) {
							if (!isEqual(exponents.get(i), 1) || !isEqual(exponents.get(j), 1)) {
								return sn;
							} else {
								foundCommon = true;
							}
						}
					}
				}

				if (tracker.isMarked(sn, RegroupTracker.MarkType.ROOT) && foundCommon) {
					return sn;
				}

				StepExpression newConstant = StepConstant.create(Math.abs(constantValue));

				if (constantList.size() > 1) {
					for (StepExpression constant : constantList) {
						constant.setColor(tracker.getColorTracker());
					}

					sb.add(SolutionStepType.MULTIPLY_CONSTANTS, tracker.getColorTracker());
					newConstant.setColor(tracker.incColorTracker());
				}

				if (constantValue < 0) {
					return minus(multiply(newConstant, nonConstant));
				} else {
					return multiply(newConstant, nonConstant);
				}
			}

			if (sn.isOperation(Operation.NROOT) && ((StepOperation) sn).getOperand(0).isOperation(Operation.MULTIPLY)) {
				tracker.addMark(((StepOperation) sn).getOperand(0), RegroupTracker.MarkType.ROOT);
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	DISTRIBUTE_POWER_OVER_PRODUCT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isOperation(Operation.MULTIPLY)) {
					StepOperation result = new StepOperation(Operation.MULTIPLY);

					so.getOperand(1).setColor(tracker.incColorTracker());
					for (StepExpression operand : (StepOperation) so.getOperand(0)) {
						operand.setColor(tracker.incColorTracker());
						result.addOperand(power(operand, so.getOperand(1)));
					}

					sb.add(SolutionStepType.DISTRIBUTE_POWER_OVER_PRODUCT);

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	POWER_OF_NEGATIVE {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isNegative()) {
					if (so.getOperand(1).isEven()) {
						StepExpression result = power(so.getOperand(0).negate(), so.getOperand(1));

						so.setColor(tracker.getColorTracker());
						result.setColor(tracker.getColorTracker());
						sb.add(SolutionStepType.EVEN_POWER_NEGATIVE, tracker.incColorTracker());

						return result;
					} else if (isOdd(so.getOperand(1))) {
						StepExpression result = power(so.getOperand(0).negate(), so.getOperand(1)).negate();

						so.setColor(tracker.getColorTracker());
						result.setColor(tracker.getColorTracker());
						sb.add(SolutionStepType.ODD_POWER_NEGATIVE, tracker.incColorTracker());

						return result;
					}
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLIFY_POWER_OF_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER) && ((StepOperation) sn).getOperand(0).isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				StepExpression exponent1 = so.getOperand(1);
				StepExpression exponent2 = ((StepOperation) so.getOperand(0)).getOperand(1);

				StepExpression gcd = StepHelper.GCD(exponent1, exponent2);

				if (!isOne(gcd)) {
					exponent1 = exponent1.quotient(gcd);
					exponent2 = exponent2.quotient(gcd);

					gcd.setColor(tracker.getColorTracker());

					StepExpression argument = ((StepOperation) so.getOperand(0)).getOperand(0);

					StepExpression result = nonTrivialPower(nonTrivialRoot(argument, exponent2), exponent1);
					sb.add(SolutionStepType.REDUCE_ROOT_AND_POWER, gcd);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.incColorTracker());
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},


	SIMPLIFY_ROOT_OF_POWER {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT) && ((StepOperation) sn).getOperand(0).isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				StepExpression exponent1 = so.getOperand(1);
				StepExpression exponent2 = ((StepOperation) so.getOperand(0)).getOperand(1);

				StepExpression gcd = StepHelper.GCD(exponent1, exponent2);

				if (!isOne(gcd)) {
					exponent1 = exponent1.quotient(gcd);
					exponent2 = exponent2.quotient(gcd);

					gcd.setColor(tracker.getColorTracker());

					StepExpression argument = ((StepOperation) so.getOperand(0)).getOperand(0);

					StepExpression result;

					if (gcd.isEven() && (exponent2 == null || !exponent2.isEven())
							&& !(argument.canBeEvaluated() && argument.getValue() > 0)) {
						result = nonTrivialRoot(nonTrivialPower(abs(argument), exponent2), exponent1);
						sb.add(SolutionStepType.REDUCE_ROOT_AND_POWER_EVEN, gcd);
					} else {
						result = nonTrivialRoot(nonTrivialPower(argument, exponent2), exponent1);
						sb.add(SolutionStepType.REDUCE_ROOT_AND_POWER, gcd);
					}

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.incColorTracker());
					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	POWER_OF_POWER {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isOperation(Operation.POWER)) {
					StepExpression result = power(((StepOperation) so.getOperand(0)).getOperand(0),
							multiply(so.getOperand(1), ((StepOperation) so.getOperand(0)).getOperand(1)));

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.POWER_OF_POWER, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	ROOT_OF_ROOT {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isOperation(Operation.NROOT)) {
					StepExpression result = root(((StepOperation) so.getOperand(0)).getOperand(0),
							multiply(so.getOperand(1), ((StepOperation) so.getOperand(0)).getOperand(1)));

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ROOT_OF_ROOT, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLE_POWERS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (isEqual(so.getOperand(1), 0)) {
					StepExpression result = StepConstant.create(1);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ZEROTH_POWER, tracker.incColorTracker());

					return result;
				}

				if (isEqual(so.getOperand(1), 1)) {
					StepExpression result = so.getOperand(0);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.FIRST_POWER, tracker.incColorTracker());

					return result;
				}

				if (so.getOperand(0).nonSpecialConstant() && so.getOperand(1).isInteger()) {
					StepExpression result = StepConstant.create(
							Math.pow(so.getOperand(0).getValue(), so.getOperand(1).getValue()));

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.EVALUATE_POWER, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLE_ROOTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.NROOT)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(1).isEven() && so.getOperand(0).nonSpecialConstant()
						&& so.getOperand(0).isNegative()) {
					StepExpression result = StepConstant.UNDEFINED;

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.EVEN_ROOT_OF_NEGATIVE, tracker.incColorTracker());

					return result;
				}

				if (isEqual(so.getOperand(0), 1)) {
					StepExpression result = StepConstant.create(1);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ROOT_OF_ONE, tracker.incColorTracker());

					return result;
				}

				if (isEqual(so.getOperand(1), 1)) {
					StepExpression result = so.getOperand(0);

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.FIRST_ROOT, tracker.incColorTracker());

					return result;
				}

				if (isOdd(so.getOperand(1).getValue()) && so.getOperand(0).isNegative()) {
					StepExpression result = minus(root(so.getOperand(0).negate(), so.getOperand(1)));

					so.setColor(tracker.getColorTracker());
					result.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.ODD_ROOT_OF_NEGATIVE, tracker.incColorTracker());

					return result;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	ABSOLUTE_VALUE_OF_POSITIVE {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.ABS)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).sign() > 0) {
					so.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.POSITIVE_UNDER_ABSOLUTE_VALUE, tracker.incColorTracker());
					return so.getOperand(0).deepCopy();
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	ABSOLUTE_VALUE_OF_NEGATIVE {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.ABS)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isNegative()) {
					so.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.NEGATIVE_UNDER_ABSOLUTE_VALUE, tracker.incColorTracker());
					return so.getOperand(0).negate();
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	EVEN_POWER_OF_ABSOLUTE_VALUE {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn.isOperation(Operation.POWER)) {
				StepOperation so = (StepOperation) sn;

				if (so.getOperand(0).isOperation(Operation.ABS)
						&& so.getOperand(1).isEven()) {
					so.setColor(tracker.getColorTracker());

					sb.add(SolutionStepType.EVEN_POWER_OF_ABSOLUTE_VALUE, tracker.incColorTracker());
					return power(((StepOperation) so.getOperand(0)).getOperand(0), so.getOperand(1));
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLIFY_ABSOLUTE_VALUES {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] strategy = new SimplificationStepGenerator[] {
					ABSOLUTE_VALUE_OF_POSITIVE,
					ABSOLUTE_VALUE_OF_NEGATIVE,
					EVEN_POWER_OF_ABSOLUTE_VALUE
			};

			return StepStrategies.implementGroup(sn, null, strategy, sb, tracker);
		}
	},

	CALCULATE_INVERSE_TRIGO {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			if (sn instanceof StepOperation && ((StepOperation) sn).isInverseTrigonometric()) {
				StepOperation so = (StepOperation) sn;

				StepExpression value = inverseTrigoLookup(so);
				if (value != null) {
					so.setColor(tracker.getColorTracker());
					value.setColor(tracker.getColorTracker());
					sb.add(SolutionStepType.EVALUATE_INVERSE_TRIGO, tracker.incColorTracker());
					return value;
				}
			}

			return StepStrategies.iterateThrough(this, sn, sb, tracker);
		}
	},

	SIMPLIFY_FRACTIONS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] strategy = new SimplificationStepGenerator[] {
					NEGATIVE_FRACTIONS,
					ELIMINATE_OPPOSITES,
					REGROUP_SUMS,
					REGROUP_PRODUCTS,
					SIMPLE_POWERS,
					FACTOR_FRACTIONS,
					FACTOR_MINUS_FROM_SUMS,
					CANCEL_FRACTION,
					CANCEL_INTEGER_FRACTION,
					TRIVIAL_FRACTIONS
			};

			return StepStrategies.implementGroup(sn, null, strategy, sb, tracker);
		}
	},

	REGROUP_PRODUCTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] strategy = new SimplificationStepGenerator[] {
					MULTIPLIED_BY_ZERO,
					MULTIPLY_NEGATIVES,
					MULTIPLY_CONSTANTS,
					COLLECT_LIKE_TERMS_PRODUCT,
					REGROUP_SUMS
			};

			return StepStrategies.implementGroup(sn, SolutionStepType.REGROUP_PRODUCTS, strategy, sb, tracker);
		}
	},

	SIMPLIFY_ROOTS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] strategy = new SimplificationStepGenerator[] {
					ROOT_OF_ROOT,
					REWRITE_INTEGER_UNDER_ROOT,
					REWRITE_POWER_UNDER_ROOT,
					SPLIT_ROOTS,
					SIMPLIFY_POWER_OF_ROOT,
					SIMPLIFY_ROOT_OF_POWER,
					SIMPLE_ROOTS
			};

			return StepStrategies.implementGroup(sn, null, strategy, sb, tracker);
		}
	},

	RATIONALIZE_DENOMINATORS {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] strategy = new SimplificationStepGenerator[]{
					POWER_OF_NEGATIVE,
					SQUARE_ROOT_MULTIPLIED_BY_ITSELF,
					COMMON_ROOT,
					DISTRIBUTE_POWER_OVER_PRODUCT,
					REGROUP_SUMS,
					REGROUP_PRODUCTS,
					SIMPLIFY_ROOTS,
					SIMPLE_POWERS,
					ExpandSteps.EXPAND_DIFFERENCE_OF_SQUARES,
					ExpandSteps.EXPAND_PRODUCTS,
					RATIONALIZE_SIMPLE_DENOMINATOR,
					RATIONALIZE_COMPLEX_DENOMINATOR
			};

			return StepStrategies.implementGroup(sn, SolutionStepType.RATIONALIZE_DENOMINATOR, strategy, sb, tracker);
		}
	},

	DECIMAL_REGROUP {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] evaluateStrategy = new SimplificationStepGenerator[] {
					RegroupSteps.DECIMAL_SIMPLIFY_ROOTS,
					RegroupSteps.DECIMAL_SIMPLIFY_FRACTIONS,
					RegroupSteps.DEFAULT_REGROUP
			};

			return StepStrategies.implementGroup(sn, null, evaluateStrategy, sb, tracker.setDecimalSimplify());
		}
	},

	WEAK_REGROUP {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] weakStrategy = new SimplificationStepGenerator[] {
					RegroupSteps.CALCULATE_INVERSE_TRIGO,
					RegroupSteps.ELIMINATE_OPPOSITES,
					RegroupSteps.DOUBLE_MINUS,
					RegroupSteps.POWER_OF_NEGATIVE,
					RegroupSteps.DISTRIBUTE_ROOT_OVER_FRACTION,
					RegroupSteps.DISTRIBUTE_POWER_OVER_PRODUCT,
					RegroupSteps.DISTRIBUTE_POWER_OVER_FRACION,
					RegroupSteps.EXPAND_ROOT,
					RegroupSteps.COMMON_ROOT,
					RegroupSteps.REWRITE_ROOT_UNDER_POWER,
					RegroupSteps.SIMPLIFY_ROOTS,
					RegroupSteps.NEGATIVE_FRACTIONS,
					RegroupSteps.TRIVIAL_FRACTIONS,
					RegroupSteps.REWRITE_COMPLEX_FRACTIONS,
					RegroupSteps.COMMON_FRACTION,
					RegroupSteps.REGROUP_SUMS,
					RegroupSteps.REGROUP_PRODUCTS,
					RegroupSteps.SIMPLE_POWERS,
					RegroupSteps.SIMPLIFY_FRACTIONS,
			};

			return StepStrategies.implementGroup(sn, null, weakStrategy, sb, tracker);
		}
	},

	DEFAULT_REGROUP {
		@Override
		public StepNode apply(StepNode sn, SolutionBuilder sb, RegroupTracker tracker) {
			SimplificationStepGenerator[] defaultStrategy = new SimplificationStepGenerator[] {
					RegroupSteps.CALCULATE_INVERSE_TRIGO,
					RegroupSteps.ELIMINATE_OPPOSITES,
					RegroupSteps.DOUBLE_MINUS,
					RegroupSteps.DISTRIBUTE_MINUS,
					RegroupSteps.POWER_OF_NEGATIVE,
					RegroupSteps.POWER_OF_POWER,
					RegroupSteps.SIMPLIFY_ABSOLUTE_VALUES,
					RegroupSteps.DISTRIBUTE_ROOT_OVER_FRACTION,
					RegroupSteps.DISTRIBUTE_POWER_OVER_PRODUCT,
					RegroupSteps.DISTRIBUTE_POWER_OVER_FRACION,
					RegroupSteps.EXPAND_ROOT,
					RegroupSteps.COMMON_ROOT,
					RegroupSteps.REWRITE_ROOT_UNDER_POWER,
					RegroupSteps.SIMPLIFY_ROOTS,
					RegroupSteps.TRIVIAL_FRACTIONS,
					RegroupSteps.NEGATIVE_FRACTIONS,
					RegroupSteps.REWRITE_COMPLEX_FRACTIONS,
					RegroupSteps.REGROUP_SUMS,
					RegroupSteps.REWRITE_AS_EXPONENTIAL,
					RegroupSteps.COMMON_FRACTION,
					RegroupSteps.REGROUP_PRODUCTS,
					RegroupSteps.SIMPLE_POWERS,
					RegroupSteps.SIMPLIFY_FRACTIONS,
					RegroupSteps.FACTOR_FRACTIONS,
					ExpandSteps.EXPAND_PRODUCTS,
					RegroupSteps.RATIONALIZE_DENOMINATORS,
					FractionSteps.ADD_FRACTIONS
			};

			// temporary hack. find some nicer solution..
			boolean expandSettings = tracker.getExpandSettings();
			tracker.setStrongExpand(false);
			StepNode temp = StepStrategies.implementGroup(sn, null, defaultStrategy, sb, tracker);
			tracker.setStrongExpand(expandSettings);
			return temp;
		}
	};

	@Override
	public boolean isGroupType() {
		return this == FACTOR_FRACTIONS
                || this == SIMPLIFY_ROOTS
                || this == SIMPLIFY_FRACTIONS
                || this == DEFAULT_REGROUP
                || this == WEAK_REGROUP
                || this == RATIONALIZE_DENOMINATORS
                || this == REGROUP_PRODUCTS
                || this == CONVERT_DECIMAL_TO_FRACTION
				|| this == SIMPLIFY_ABSOLUTE_VALUES;
	}
}
