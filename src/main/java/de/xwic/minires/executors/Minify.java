package de.xwic.minires.executors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

import de.xwic.minires.MiniresAbstractMojo;

/**
 *
 */
@Mojo (name = "minify")
public final class Minify extends MiniresAbstractMojo {

	@Parameter
	private boolean verbose;

	@Parameter
	private String minifiedCssFile;

	@Parameter
	private String minifiedJsFile;

	private final List<String> cssFiles = new ArrayList<String>();
	private final List<String> jsFiles = new ArrayList<String>();

	/*
	 * (non-Javadoc)
	 *
	 * @see de.xwic.minires.MiniresAbstractMojo#safelyExecute()
	 */
	@Override
	protected void safelyExecute() throws MojoExecutionException, MojoFailureException {
		super.safelyExecute();
		try {

			generateMinified(minifiedCssFile, cssFiles, new IoProcessor() {
				public void process(final Reader in, final Writer out) throws IOException {
					new CssCompressor(in).compress(out, 8000);
				}
			});

			generateMinified(minifiedJsFile, jsFiles, new IoProcessor() {
				public void process(final Reader in, final Writer out) throws IOException {
					new JavaScriptCompressor(in, new JustLogErrorReporter()).compress(out, 8000, false, verbose, true, true);
				}
			});

		} catch (final IOException e) {
			throw new MojoExecutionException("Failed to execute 'minify'", e);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.xwic.minires.MiniresAbstractMojo#checkParameters()
	 */
	@Override
	protected void checkParameters() throws MojoExecutionException {
		ifEmpty(minifiedJsFile, "Missing minifiedJsFile");
		ifEmpty(minifiedCssFile, "Missing minifiedCssFile");
	}

	/**
	 * @param targetFile
	 * @param files
	 * @param processor
	 * @throws IOException
	 */
	private void generateMinified(final String targetFile, final List<String> files, final IoProcessor processor) throws IOException {

		final File destFile = new File(outputDir, targetFile);
		if (destFile.exists()) {
			getLog().info(destFile.getAbsolutePath() + " already exists, deleting...");
			destFile.delete();
		}

		final StringBuilder sb = new StringBuilder();

		// iterate over files and write it in.
		for (final String file : files) {
			loadFile(file, sb);
		}

		Reader in = null;
		Writer out = null;
		try {
			in = new StringReader(sb.toString());
			out = new BufferedWriter(new FileWriter(destFile));
			processor.process(in, out);
		} finally {
			if (null != out) {
				out.close();
			}
			if (null != in) {
				in.close();
			}
		}
	}

	/**
	 * @param file
	 * @param sb
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void loadFile(final String file, final StringBuilder sb) throws FileNotFoundException, IOException {
		final File daFile = new File(inputDir, file);
		if (!daFile.exists()) {
			throw new FileNotFoundException("The file " + daFile.getAbsolutePath() + " can not be found.");
		}

		sb.append("/* content: " + file + " */\n");
		sb.append(FileUtils.fileRead(daFile, "UTF-8")).append("\n");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.xwic.minires.MiniresAbstractMojo#processLine(java.lang.String, boolean, java.lang.StringBuilder)
	 */
	@Override
	protected boolean processLine(final String line, final boolean inside, final StringBuilder sb) {
		if (!inside) {
			if (line.indexOf(processStartTag) != -1) {
				// add the static minified line references
				sb.append("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + minifiedCssFile + "\">").append(
						"\n");
				sb.append("<SCRIPT LANGUAGE=\"JavaScript\" SRC=\"" + minifiedJsFile + "\"></SCRIPT>").append("\n");
				return true;
			}
			sb.append(line).append("\n");
		} else {
			if (line.indexOf(processEndTag) != -1) {
				return false;
			}
			detectResource(line, "<LINK", "HREF=\"", cssFiles);
			detectResource(line, "<SCRIPT", "SRC=\"", jsFiles);
		}
		return inside;
	}

	/**
	 * @param line
	 * @param linePrefix
	 * @param refAttr
	 * @param list
	 */
	private void detectResource(final String line, final String linePrefix, final String refAttr, final List<String> list) {
		final String ucLine = line.toUpperCase();
		final int idx = ucLine.indexOf(linePrefix);
		if (idx == -1) { // is a resource reference
			return;
		}
		final int idxHref = ucLine.indexOf(refAttr, idx);
		if (idxHref == -1) {
			return;
		}
		final int idxEnd = ucLine.indexOf("\"", idxHref + refAttr.length());
		if (idxEnd == -1) {
			return;
		}
		final String resFile = line.substring(idxHref + refAttr.length(), idxEnd);
		getLog().info("Detected resource: " + resFile);
		list.add(resFile);
	}

	/**
	 *
	 */
	public interface IoProcessor {

		/**
		 * @param in
		 * @param out
		 * @throws IOException
		 */
		void process(Reader in, Writer out) throws IOException;

	}

	/**
	 *
	 */
	private class JustLogErrorReporter implements ErrorReporter {

		/*
		 * (non-Javadoc)
		 *
		 * @see org.mozilla.javascript.ErrorReporter#warning(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public void warning(final String arg0, final String arg1, final int arg2, final String arg3, final int arg4) {
			getLog().warn("warning: '" + arg0 + "' // '" + arg1 + "' // '" + arg2 + "' // '" + arg3 + "' // '" + arg4 + "'");
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.mozilla.javascript.ErrorReporter#runtimeError(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public EvaluatorException runtimeError(final String arg0, final String arg1, final int arg2, final String arg3, final int arg4) {
			getLog().error("runtimeError: " + arg0 + "' // '" + arg1 + "' // '" + arg2 + "' // '" + arg3 + "' // '" + arg4 + "'");
			return null;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.mozilla.javascript.ErrorReporter#error(java.lang.String, java.lang.String, int, java.lang.String, int)
		 */
		public void error(final String arg0, final String arg1, final int arg2, final String arg3, final int arg4) {
			getLog().error("error: " + arg0 + "' // '" + arg1 + "' // '" + arg2 + "' // '" + arg3 + "' // '" + arg4 + "'");
		}

	}

}
