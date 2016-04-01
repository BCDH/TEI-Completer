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

import com.evolvedbinary.xpath.parser.ast.QNameW;

import static com.evolvedbinary.xpath.parser.ast.QNameW.WILDCARD;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Adam Retter, Evolved Binary Ltd
 */
public class XPathUtilTest {

    /**
     * Tests that prove the equivalence of two XPath expressions
     */
    @Test
    public void isSubset() {

        assertFalse(XPathUtil.isSubset("/a/b/c", "//a"));
        assertFalse(XPathUtil.isSubset("//a", "//c"));
        assertFalse(XPathUtil.isSubset("//a", "/a"));
        assertFalse(XPathUtil.isSubset("//a", "/a/b/c"));
        assertFalse(XPathUtil.isSubset("//a", "//b//a"));

        assertTrue(XPathUtil.isSubset("/a/b/c", "//c"));
        assertTrue(XPathUtil.isSubset("/a", "//a"));
        assertTrue(XPathUtil.isSubset("/b/a", "//a"));
        assertTrue(XPathUtil.isSubset("/b//a", "//a"));
        assertTrue(XPathUtil.isSubset("//b//a", "//b//a"));
        assertTrue(XPathUtil.isSubset("//b//a", "//a"));
        assertTrue(XPathUtil.isSubset("//b//c//a", "//a"));

        assertTrue(XPathUtil.isSubset("/TEI/text/body/p/w", "//w"));
        assertTrue(XPathUtil.isSubset("/TEI/text[1]/body[1]/p[2]/w[7]", "//w"));

        assertTrue(XPathUtil.isSubset("/TEI/text/body/p/w/@lemma", "//w/@lemma"));
        assertTrue(XPathUtil.isSubset("/TEI/text[1]/body[1]/p[2]/w[7]/@lemma", "//w/@lemma"));

        assertTrue(XPathUtil.isSubset("/TEI/text[1]/body[1]/quote[1]/rs[1]/@ref", "//w/@ref"));

        //TODO(AR) consider whether these should return true
//        assertTrue(XPathUtil.isSubset("/TEI/text/body/p/w/@lemma", "//w"));
//        assertTrue(XPathUtil.isSubset("/TEI/text[1]/body[1]/p[2]/w[7]/@lemma", "//w"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void isSubset_illegalSubset() {
        XPathUtil.isSubset("a/b", "//a");
    }

    @Test(expected = IllegalArgumentException.class)
    public void isSubset_illegalSuperset() {
        XPathUtil.isSubset("//a", "a/b");
    }

    @Test
    public void isAbsolutePathExpr() {
        assertTrue(XPathUtil.isAbsolutePathExpr("/a"));
        assertTrue(XPathUtil.isAbsolutePathExpr("/a/b"));
        assertTrue(XPathUtil.isAbsolutePathExpr("//a"));
        assertTrue(XPathUtil.isAbsolutePathExpr("//a/b"));

        assertFalse(XPathUtil.isAbsolutePathExpr("a"));
        assertFalse(XPathUtil.isAbsolutePathExpr("a/b"));
        assertFalse(XPathUtil.isAbsolutePathExpr("a//b"));
        assertFalse(XPathUtil.isAbsolutePathExpr("./a"));
        assertFalse(XPathUtil.isAbsolutePathExpr("./a/b"));
        assertFalse(XPathUtil.isAbsolutePathExpr(".//a/b"));
    }

    @Test
    public void isSubsetNameTest() {
        assertFalse(XPathUtil.isSubsetNameTest(null, new QNameW("a")));
        assertFalse(XPathUtil.isSubsetNameTest(new QNameW("b"), new QNameW("a")));
        assertFalse(XPathUtil.isSubsetNameTest(new QNameW(WILDCARD), new QNameW("a")));
        assertFalse(XPathUtil.isSubsetNameTest(new QNameW(WILDCARD, WILDCARD), new QNameW("a")));
        assertFalse(XPathUtil.isSubsetNameTest(new QNameW(WILDCARD), new QNameW("ns:a")));
        assertFalse(XPathUtil.isSubsetNameTest(new QNameW(WILDCARD, WILDCARD), new QNameW("ns:a")));

        assertTrue(XPathUtil.isSubsetNameTest(null, null));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW("a"), null));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW(WILDCARD), new QNameW(WILDCARD)));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW(WILDCARD, WILDCARD), new QNameW(WILDCARD)));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW(WILDCARD), new QNameW(WILDCARD, WILDCARD)));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW("a"), new QNameW(WILDCARD)));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW("a"), new QNameW(WILDCARD, WILDCARD)));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW("ns:a"), new QNameW(WILDCARD)));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW("ns:a"), new QNameW(WILDCARD, WILDCARD)));
        assertTrue(XPathUtil.isSubsetNameTest(new QNameW("a"), new QNameW("a")));
    }
}
