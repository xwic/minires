package de.xwic.minires;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 *
 */
public abstract class MiniresAbstractMojo extends AbstractMojo {

	@Parameter
	protected String processStartTag;

	@Parameter
	protected String processEndTag;

	@Parameter
	protected String inputDir;

	@Parameter
	protected String outputDir;

	@Parameter
	protected String inputHeaderFile;

	@Parameter
	protected String outputHeaderFile;

	/*
	 * (non-Javadoc)
	 *
	 * @see org.apache.maven.plugin.AbstractMojo#execute()
	 */
	public final void execute() throws MojoExecutionException, MojoFailureException {
		ifEmpty(processStartTag, "Missing processStartTag");
		ifEmpty(processEndTag, "Missing processEndTag");

		ifEmpty(inputDir, "Missing inputDir");
		ifEmpty(outputDir, "Missing outputDir");

		ifEmpty(inputHeaderFile, "Missing inputHeaderFile");
		ifEmpty(outputHeaderFile, "Missing outputHeaderFile");
		checkParameters();
		safelyExecute();
	}

	/**
	 * @throws IOException
	 */
	private void updatePageFile() throws IOException {
		final File src = new File(inputHeaderFile);
		if (!src.exists()) {
			throw new FileNotFoundException(inputHeaderFile + " does not exist, please check what you are doing!");
		}

		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(src));

			boolean inside = false;
			String line;
			final StringBuilder sb = new StringBuilder();
			while ((line = in.readLine()) != null) {
				inside = processLine(line, inside, sb);
			}

			FileUtils.fileWrite(outputHeaderFile, "UTF-8", sb.toString());
			final Log log = getLog();
			log.info("Input file: " + inputHeaderFile);
			log.info("Output file: " + outputHeaderFile);

		} finally {
			if (null != in) {
				in.close();
			}
		}
	}
	/**
	 * @throws MojoExecutionException
	 * @throws MojoFailureException
	 */
	protected void safelyExecute() throws MojoExecutionException, MojoFailureException {
		try {
			updatePageFile();
		} catch (final IOException e) {
			throw new MojoExecutionException("Failed to update the page file.", e);
		}
	}

	/**
	 * @param line
	 * @param inside
	 * @param sb
	 * @return
	 * @throws IOException
	 */
	protected abstract boolean processLine(final String line, boolean inside, final StringBuilder sb) throws IOException;

	/**
	 * @throws MojoExecutionException
	 */
	protected void checkParameters() throws MojoExecutionException {
	}

	/**
	 * @param string
	 * @param message
	 * @throws MojoExecutionException
	 */
	protected final static void ifEmpty(final String string, final String message) throws MojoExecutionException {
		if (StringUtils.isEmpty(string)) {
			throw new MojoExecutionException(message);
		}
	}

}
