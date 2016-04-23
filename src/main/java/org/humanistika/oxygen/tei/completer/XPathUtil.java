/**
 * TEI Completer
 * An Oxygen XML Editor plugin for customizable attribute and value completion for TEI P5 documents
 * Copyright (C) 2016 Belgrade Center for Digital Humanities
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.humanistika.oxygen.tei.completer;

import com.evolvedbinary.xpath.parser.ast.*;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

/**
 * @author Adam Retter, Evolved Binary Ltd
 * @version 1.0
 * @serial 20160316
 */
public class XPathUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(XPathUtil.class);

    public static Expr parseXPath(final String xpath) {

        ByteArrayOutputStream osErr = null;
        PrintStream err = null;
        ByteArrayOutputStream osOut = null;
        PrintStream out = null;

        try {
            osErr = new ByteArrayOutputStream();
            err = new PrintStream(osErr, true, "UTF-8");

            if (LOGGER.isDebugEnabled()) {
                osOut = new ByteArrayOutputStream();
                out = new PrintStream(osOut, true, "UTF-8");
            }

            final Expr result = com.evolvedbinary.xpath.parser.XPathUtil.parseXPath(xpath, out, err);

            final String strOut = asString(out, osOut);
            if(!strOut.isEmpty()) {
                LOGGER.debug(strOut);
            }

            final String strErr = asString(err, osErr);
            if(!strErr.isEmpty()) {
                LOGGER.error(strErr);
            }

            return result;
        } catch (final UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            return null;
        } finally {
            close(out, osOut);
            close(err, osErr);
        }
    }

    private static String asString(final PrintStream ps, ByteArrayOutputStream os) throws UnsupportedEncodingException {
        if(ps != null) {
            ps.flush();
            return os.toString("UTF-8");
        } else {
            return "";
        }
    }

    private static void close(final PrintStream ps, final ByteArrayOutputStream os) {
        if(ps != null) {
            ps.close();
        }
        if(os != null) {
            try {
                os.close();
            } catch (final IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    public static boolean isSubset(final String subset, final String superset) {
        return isSubset(parseXPath(subset), parseXPath(superset));
    }

    /**
     * Determines if one expression would produce a subset of the results of
     * another expression
     *
     * @param subset An expression which to test is a subset of the superset expression
     * @param superset An expression which to test is the superset of the subset expression
     *
     * @return true if the subset parameter is a valid subset of the superset parameter
     *
     * @throws IllegalArgumentException If the subset or superset expressions are not absolute paths
     */
    public static boolean isSubset(final Expr subset, final Expr superset) {
        if(!isAbsolutePathExpr(subset)) {
            throw new IllegalArgumentException("Subset expression must be an absolute path expression");
        }
        if(!isAbsolutePathExpr(superset)) {
            throw new IllegalArgumentException("Superset expression must be an absolute path expression");
        }

        final List<? extends StepExpr> subsetSteps = getSteps(subset);
        final List<? extends StepExpr> supersetSteps = getSteps(superset);

        int subsetIdx = 0, supersetIdx = 0;

        while(subsetIdx < subsetSteps.size() && supersetIdx < supersetSteps.size()) {
            final StepExpr subsetStep = subsetSteps.get(subsetIdx);
            final StepExpr supersetStep = supersetSteps.get(supersetIdx);

            if(subsetStep.equals(PathExpr.SLASH_ABBREV)) {
                if(supersetStep.equals(PathExpr.SLASH_ABBREV)) {
                    subsetIdx++; supersetIdx++;
                    continue;
                } else if(supersetStep.equals(PathExpr.SLASH_SLASH_ABBREV)) {
                    //consume all subsetSteps of superset //X
                    final StepExpr supersetNextStep = supersetSteps.get(++supersetIdx);
                    while(subsetIdx < subsetSteps.size() - 1) {
                        if(isSubsetStep((AxisStep)subsetSteps.get(++subsetIdx), (AxisStep)supersetNextStep)) {
                            subsetIdx++;
                            break;
                        }
                    }
                    supersetIdx++;
                }

            } else if(subsetStep.equals(PathExpr.SLASH_SLASH_ABBREV)) {
                if(supersetStep.equals(PathExpr.SLASH_SLASH_ABBREV)) {
                    //consume all subsetSteps of superset //X
                    final StepExpr supersetNextStep = supersetSteps.get(++supersetIdx);
                    while(subsetIdx < subsetSteps.size() - 1) {
                        if(isSubsetStep((AxisStep)subsetSteps.get(++subsetIdx), (AxisStep)supersetNextStep)) {
                            subsetIdx++;
                            break;
                        }
                    }
                    supersetIdx++;
                } else if(supersetStep.equals(PathExpr.SLASH_ABBREV)) {
                    break;
                }
            } else if(subsetStep instanceof AxisStep && supersetStep instanceof AxisStep) {
                if(isSubsetStep((AxisStep)subsetStep, (AxisStep)supersetStep)) {
                    subsetIdx++; supersetIdx++;
                    continue;
                } else {
                    break; //this should always cause the last condition in this function to evaluate to false!
                }
            }
        }

        return subsetIdx > subsetSteps.size() - 1;
    }

    /**
     * Check if one axis step is a subset of another
     *
     * NOTE - currently Ignores predicate lists
     *
     * @param subset An axis step which to test is a subset of the superset axis step
     * @param superset An axis step which to test is the superset of the axis step expression
     *
     * @return true if the subset parameter is a valid subset of the superset parameter
     */
    public static boolean isSubsetStep(final AxisStep subset, final AxisStep superset) {
            final Step subsetStep = subset.getStep();
            final Step supersetStep = superset.getStep();

            final Axis subsetAxis = subsetStep.getAxis();
            final NodeTest subsetNodeTest = subsetStep.getNodeTest();

            final Axis supersetAxis = supersetStep.getAxis();
            final NodeTest supersetNodeTest = supersetStep.getNodeTest();

            return isSubsetAxis(subsetAxis, supersetAxis)
                    && isSubsetNodeTest(subsetNodeTest, subsetAxis, supersetNodeTest);
    }

    public static boolean isSubsetAxis(final Axis subset, final Axis superset) {
        if(subset.getDirection() == superset.getDirection()) {
            return true;
        } else {
            switch (subset.getDirection()) {
                case FOLLOWING_SIBLING:
                    return superset.getDirection() == Axis.Direction.FOLLOWING;

                case PRECEDING_SIBLING:
                    return superset.getDirection() == Axis.Direction.PRECEDING;

                //TODO(AR) do we need CHILD / DESCENDANT
                //TODO(AR) do we need PARENT / ANCESTOR
                //TODO(AR) do we need DESCENDANT / DESCENDANT_OR_SELF

                default:
                    return false;
            }
        }
    }

    public static boolean isSubsetNodeTest(final NodeTest subset, final Axis subsetAxis, final NodeTest superset) {
        if(subset.equals(superset)) {
            return true;
        } else if(superset instanceof AnyKindTest) {
            return true;
        } else if(subset instanceof NameTest && superset instanceof NameTest) {
            return isSubsetNameTest(((NameTest)subset).getName(), ((NameTest)superset).getName());
        } else if(subset instanceof NameTest && superset instanceof KindTest) {
            if(subsetAxis.equals(Axis.ATTRIBUTE) && superset instanceof AttributeTest) {
                return isSubsetNameTest(((NameTest)subset).getName(), ((AttributeTest)superset).getName());
            } else if(subsetAxis.equals(Axis.ATTRIBUTE) && superset instanceof SchemaAttributeTest) {
                return isSubsetNameTest(((NameTest)subset).getName(), ((SchemaAttributeTest)superset).getName());
            } else if(superset instanceof ElementTest) {
                return isSubsetNameTest(((NameTest)subset).getName(), ((ElementTest)superset).getName());
            } else if(superset instanceof SchemaElementTest) {
                return isSubsetNameTest(((NameTest)subset).getName(), ((SchemaElementTest)superset).getName());
            }
        }

        return false;
    }

    public static boolean isSubsetNameTest(@Nullable final QNameW subset, @Nullable final QNameW superset) {
        if(subset == null && superset == null) {
            return true;
        } else if(superset == null) {
            return true;
        } else if(subset == null) {
            return false;
        } else if(subset.equals(superset)) {
            return true;
        } else if((superset.getPrefix() == null || superset.getPrefix().equals(QNameW.WILDCARD)) && superset.getLocalPart().equals(QNameW.WILDCARD)) {
            return true;
        } else {
            return (subset.getPrefix() == null ? "" : subset.getPrefix()).equals(superset.getPrefix() == null ? "" : superset.getPrefix())
                    && superset.getLocalPart() != null && superset.getLocalPart().equals(QNameW.WILDCARD);
        }
    }

    public static boolean isAbsolutePathExpr(final String expr) {
        return isAbsolutePathExpr(parseXPath(expr));
    }

    public static boolean isAbsolutePathExpr(final Expr expr) {
        final List<? extends StepExpr> steps = getSteps(expr);
        if(steps.size() > 1) {
            final StepExpr step1 = steps.get(0);
            return step1.equals(PathExpr.SLASH_ABBREV) || step1.equals(PathExpr.SLASH_SLASH_ABBREV);
        }
        return false;
    }

    public static List<? extends StepExpr> getSteps(final Expr expr) {
        final List<? extends ASTNode> exprSingles = expr.getExprSingles();
        if(exprSingles.size() == 1) {
            final ASTNode exprSingle = exprSingles.get(0);
            if (exprSingle instanceof ValueExpr) {
                final PathExpr pathExpr = (PathExpr) ((ValueExpr) exprSingle).getPathExpr();
                return pathExpr.getSteps();
            }
        }
        return Collections.emptyList();
    }
}

