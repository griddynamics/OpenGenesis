package com.griddynamics.genesis.test.suites;

import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;

import java.util.Arrays;
import java.util.List;

import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.embedder.UnmodifiableEmbedderControls;
import org.jbehave.core.io.StoryFinder;
import org.testng.annotations.Test;

/**
 * Class which is an entry point to REST API test stories.
 * It should be used if tests are run via Java from console.
 * 
 * @author ybaturina
 *
 */
public class CIStoriesTestSuite extends AllStoriesTestSuite{

	public CIStoriesTestSuite(){
		super();
	}
	
	@Override
	@Test
	public void run() throws Throwable {
		Embedder embedder = configuredEmbedder();
		EmbedderControls newControls = new UnmodifiableEmbedderControls(
				new EmbedderControls().doGenerateViewAfterStories(true)
						.doIgnoreFailureInStories(true)
						.doIgnoreFailureInView(false)
						.useStoryTimeoutInSecs(600));

		embedder.useEmbedderControls(newControls);
		embedder.useMetaFilters(Arrays.asList("-skip"));
		try {
			embedder.runStoriesAsPaths(storyPaths());
		} finally {
			embedder.generateCrossReference();
		}
	}
	
	@Override
	protected List<String> storyPaths() {
		return new StoryFinder().findPaths(
				codeLocationFromClass(this.getClass()), 
				"**/Run*.story", "");
	}
	
}
