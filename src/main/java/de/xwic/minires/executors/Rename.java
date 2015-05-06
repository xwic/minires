package de.xwic.minires.executors;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import de.xwic.minires.MiniresAbstractMojo;

/**
 *
 */
@Mojo (name = "rename")
public class Rename extends MiniresAbstractMojo {

	@Parameter
	private String versionTag;

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.xwic.minires.MiniresAbstractMojo#checkParameters()
	 */
	@Override
	protected void checkParameters() throws MojoExecutionException {
		ifEmpty(versionTag, "Missing versionTag");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.xwic.minires.MiniresAbstractMojo#processLine(java.lang.String, boolean, java.lang.StringBuilder)
	 */
	@Override
	protected boolean processLine(final String line, final boolean inside, final StringBuilder sb) throws IOException {
		String resourceLine = line;
		if (!inside) {
			if (line.indexOf(processStartTag) != -1) {
				return true;
			}
		} else {
			if (line.indexOf(processEndTag) != -1) {
				return false;
			}
			resourceLine = detectAndRenameResource(line, "<LINK", "HREF=\"");
			resourceLine = detectAndRenameResource(line, "<SCRIPT", "SRC=\"");
		}
		sb.append(resourceLine).append("\n");
		return inside;
	}

	/**
	 * @param line
	 * @param linePrefix
	 * @param refAttr
	 * @return
	 * @throws IOException
	 */
	private String detectAndRenameResource(final String line, final String linePrefix, final String refAttr) throws IOException {

		final String resFile = extractResourceName(line, linePrefix, refAttr);
		if (line.equals(resFile)) {
			return line;
		}

		final File currentFile = new File(inputDir, resFile);

		if (!currentFile.exists()) {
			return line;
		}

		final String newName;
		final int idxExt = resFile.lastIndexOf(".");
		if (idxExt > -1) {
			newName = resFile.substring(0, idxExt) + "_" + versionTag + resFile.substring(idxExt);
		} else {
			// if no extension from whatever reason
			newName = resFile + "_" + versionTag;
		}
		final File newFile = new File(outputDir, newName);
		FileUtils.copyFile(currentFile, newFile);
		getLog().info("Copied '" + resFile + "' to '" + newName + "'");
		return line.replace(resFile, newName);
	}

	/**
	 * @param line
	 * @param linePrefix
	 * @param refAttr
	 * @return
	 */
	private static String extractResourceName(final String line, final String linePrefix, final String refAttr) {
		final String lowerCaseLine = line.toLowerCase();

		final int idx = lowerCaseLine.indexOf(linePrefix.toLowerCase());
		if (idx == -1) { // is a resource reference
			return line;
		}
		final int idxHref = lowerCaseLine.indexOf(refAttr.toLowerCase(), idx);
		if (idxHref == -1) {
			return line;
		}
		final int idxEnd = lowerCaseLine.indexOf("\"", idxHref + refAttr.length());
		if (idxEnd == -1) {
			return line;
		}
		return line.substring(idxHref + refAttr.length(), idxEnd);
	}

}
