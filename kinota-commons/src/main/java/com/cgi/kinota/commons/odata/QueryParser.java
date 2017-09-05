/**
 * Kinota (TM) Copyright (C) 2017 CGI Group Inc.
 *
 * Licensed under GNU Lesser General Public License v3.0 (LGPLv3);
 * you may not use this file except in compliance with the License.
 *
 * This software is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * v3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License v3.0 for more details.
 *
 * You can receive a copy of the GNU Lesser General Public License
 * from:
 *
 * https://www.gnu.org/licenses/lgpl-3.0.en.html
 *
 */

package com.cgi.kinota.commons.odata;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Created by bmiles on 5/23/17.
 */
public class QueryParser {

    public static ODataQuery parseQuery(String expr) throws ODataQueryException {
        CharStream input = CharStreams.fromString(expr);
        ODataLexer lexer = new ODataLexer(input);
        // Configure lexer
        lexer.removeErrorListeners();
        lexer.addErrorListener(ODataQueryErrorListener.INSTANCE);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ODataParser parser = new ODataParser(tokens);
        // Configure parser
        parser.removeErrorListeners();
        parser.addErrorListener(ODataQueryErrorListener.INSTANCE);

        ParseTree tree = parser.query();
        // Create a generic parse tree walker that can trigger callbacks
        ParseTreeWalker walker = new ParseTreeWalker();
        // Walk the tree created during the parser, trigger callbacks
        ODataQueryListener queryGenerator = new ODataQueryListener();
        walker.walk(queryGenerator, tree);
        return queryGenerator.query;
    }
}
