package com.griddynamics.genesis.test.suites;

import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.embedder.EmbedderControls;
import org.jbehave.core.embedder.UnmodifiableEmbedderControls;
import org.testng.annotations.Test;

/**
 * Class which is an entry point to REST API test stories.
 * It should be used if tests are run via Maven.
 * 
 * @author ybaturina
 *
 */
public class RestApiStoriesTestSuite extends AllStoriesTestSuite{

	public RestApiStoriesTestSuite(){
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
		try {
			embedder.runStoriesAsPaths(storyPaths());
		} finally {
			embedder.generateCrossReference();
		}
	}
	
}
