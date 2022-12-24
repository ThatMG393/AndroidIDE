/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itsaky.androidide.editor.language.java;

import androidx.annotation.NonNull;

import com.itsaky.androidide.editor.language.CommonSymbolPairs;
import com.itsaky.androidide.editor.language.IDELanguage;
import com.itsaky.androidide.editor.language.BraceHandler;
import com.itsaky.androidide.editor.language.CommonCompletionProvider;
import com.itsaky.androidide.lexers.java.JavaLexer;
import com.itsaky.androidide.lexers.java.JavaParser;
import com.itsaky.androidide.lsp.api.ILanguageServer;
import com.itsaky.androidide.lsp.api.ILanguageServerRegistry;
import com.itsaky.androidide.lsp.java.JavaLanguageServer;
import com.itsaky.androidide.utils.ILogger;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.StringReader;

import io.github.rosemoe.sora.lang.analysis.AnalyzeManager;
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler;
import io.github.rosemoe.sora.text.ContentReference;
import io.github.rosemoe.sora.util.MyCharacter;
import io.github.rosemoe.sora.widget.SymbolPairMatch;

public class JavaLanguage extends IDELanguage {

  private static final ILogger LOG = ILogger.newInstance("JavaLanguage");
  private final NewlineHandler[] newlineHandlers;
  private JavaAnalyzer analyzer;

  public JavaLanguage() {
    final var server = getLanguageServer();
    this.analyzer = new JavaAnalyzer();

    this.newlineHandlers = new NewlineHandler[1];
    this.newlineHandlers[0] = new BraceHandler(this::getIndentAdvance, this::useTab);
  }

  @Override
  protected ILanguageServer getLanguageServer() {
    return ILanguageServerRegistry.getDefault().getServer(JavaLanguageServer.SERVER_ID);
  }

  @Override
  protected boolean checkIsCompletionChar(final char c) {
    return MyCharacter.isJavaIdentifierPart(c) || c == '.';
  }

  @Override
  public int getIndentAdvance(@NonNull String line) {
    try {
      JavaLexer lexer = new JavaLexer(CharStreams.fromReader(new StringReader(line)));
      Token token;
      int advance = 0;
      while (((token = lexer.nextToken()) != null && token.getType() != token.EOF)) {
        switch (token.getType()) {
          case JavaLexer.LBRACE:
            advance++;
            break;
          case JavaParser.RBRACE:
            advance--;
            break;
        }
      }
      advance = Math.max(0, advance);
      return advance * getTabSize();
    } catch (Throwable e) {
      LOG.error("Error calculating indent advance", e);
    }

    return 0;
  }

  @NonNull
  @Override
  public AnalyzeManager getAnalyzeManager() {
    return analyzer;
  }

  @Override
  public int getInterruptionLevel() {
    return INTERRUPTION_LEVEL_SLIGHT;
  }

  @Override
  public int getIndentAdvance(@NonNull ContentReference content, int line, int column) {
    return getIndentAdvance(content.getLine(line).substring(0, column));
  }

  @Override
  public SymbolPairMatch getSymbolPairs() {
    return new JavaSymbolPairs();
  }

  @Override
  public NewlineHandler[] getNewlineHandlers() {
    return newlineHandlers;
  }

  @Override
  public void destroy() {
    analyzer = null;
  }

  public static class JavaSymbolPairs extends CommonSymbolPairs {
    public JavaSymbolPairs() {
      super();
      super.putPair('<', new SymbolPairMatch.SymbolPair("<", ">"));
    }
  }
}
