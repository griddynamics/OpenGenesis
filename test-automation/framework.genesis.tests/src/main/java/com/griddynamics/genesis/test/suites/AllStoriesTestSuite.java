package com.griddynamics.genesis.test.suites;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;
import static org.jbehave.core.reporters.Format.CONSOLE;
import static org.jbehave.core.reporters.Format.TXT;
import static org.jbehave.core.reporters.Format.HTML;
import static org.jbehave.core.reporters.Format.XML;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.jbehave.core.ConfigurableEmbedder;
import org.jbehave.core.Embeddable;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.StoryControls;
import org.jbehave.core.failures.FailingUponPendingStep;
import org.jbehave.core.failures.SilentlyAbsorbingFailure;
import org.jbehave.core.i18n.LocalizedKeywords;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.model.ExamplesTableFactory;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.InstanceStepsFactory;
import org.jbehave.core.steps.ParameterConverters;
import org.testng.annotations.Test;

import com.griddynamics.genesis.tools.converters.NullAndEmptyStringConverter;

/**
 * Base class which provides ability to run tests in stories with TestNG framework.
 * It also configures stories execution, test-reports format, stories paths
 * and step methods used in JBehave test scenarios
 * 
 * All other classes with stories configuration should be inherited from this class
 *
 * @author mlykosova, ybaturina
 */
public class AllStoriesTestSuite extends ConfigurableEmbedder{

	public AllStoriesTestSuite() {

	}

	@Test
    public void run() throws Throwable {
        Embedder embedder = configuredEmbedder();
        try {
            embedder.runStoriesAsPaths(storyPaths());
        } finally {
            embedder.generateCrossReference();
        }
    }

	@Override
	public Configuration configuration() {
		Class<? extends Embeddable> embeddableClass = this.getClass();
		// Start from default ParameterConverters instance
		ParameterConverters parameterConverters = new ParameterConverters();
		// factory to allow parameter conversion and loading from external
		// resources (used by StoryParser too)
		ExamplesTableFactory examplesTableFactory = new ExamplesTableFactory(
				new LocalizedKeywords(),
				new LoadFromClasspath(embeddableClass), parameterConverters);
		// and add custom converters
		parameterConverters.addConverters(
				new ParameterConverters.DateConverter(new SimpleDateFormat(
						"dd-MM-yyyy")),
				new ParameterConverters.ExamplesTableConverter(
						examplesTableFactory), new ParameterConverters.BooleanConverter(),
						new NullAndEmptyStringConverter());

		return new MostUsefulConfiguration()
				.useFailureStrategy(new SilentlyAbsorbingFailure())
				.useStoryLoader(new LoadFromClasspath(embeddableClass))
				.useStoryParser(new RegexStoryParser(examplesTableFactory))
				.usePendingStepStrategy(new FailingUponPendingStep())
				.useStoryReporterBuilder(
						new StoryReporterBuilder()
								.withCodeLocation(codeLocationFromClass(embeddableClass))
								.withDefaultFormats().withFailureTrace(false)
								.withFormats(CONSOLE,XML,HTML,TXT))
				.useParameterConverters(parameterConverters)
				.useStoryControls(new StoryControls().doSkipBeforeAndAfterScenarioStepsIfGivenStory(true).
						doSkipScenariosAfterFailure(false).doResetStateBeforeStory(false).doResetStateBeforeScenario(true));
	}

	protected List<String> storyPaths() {
		
		return new StoryFinder().findPaths(
				codeLocationFromClass(this.getClass()), 
				"**/Run*.story", "");
	}
	
	@Override
	public List<CandidateSteps> candidateSteps() {
		List<Object> obj = new ArrayList<Object>();
		try {
			String stepsPath = System.getProperty("stepsPath") != null ? System
					.getProperty("stepsPath") : "com.griddynamics.genesis.test.steps";
			Class[] classes = getClasses(stepsPath);
			for (int i = 0; i < classes.length; i++) {
				Constructor constr = classes[i].getConstructor(null);
				obj.add(constr.newInstance(null));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new InstanceStepsFactory(configuration(), obj)
				.createCandidateSteps();
	}
	
	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private static Class[] getClasses(String packageName)
			throws ClassNotFoundException, IOException {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		assert classLoader != null;
		String path = packageName.replace('.', '/');
		Enumeration<URL> resources = classLoader.getResources(path);
		List<File> dirs = new ArrayList<File>();
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			dirs.add(new File(resource.getFile()));
		}
		ArrayList<Class> classes = new ArrayList<Class>();
		for (File directory : dirs) {
			classes.addAll(findClasses(directory, packageName));
		}
		return classes.toArray(new Class[classes.size()]);
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<Class> findClasses(File directory, String packageName)
			throws ClassNotFoundException {
		List<Class> classes = new ArrayList<Class>();
		if (!directory.exists()) {
			return classes;
		}
		File[] files = directory.listFiles();
		for (File file : files) {
			if (file.isDirectory()) {
				assert !file.getName().contains(".");
				classes.addAll(findClasses(file,
						packageName + "." + file.getName()));
			} else if (file.getName().endsWith(".class")) {
				classes.add(Class.forName(packageName
						+ '.'
						+ file.getName().substring(0,
								file.getName().length() - 6)));
			}
		}
		return classes;
	}
    
}
