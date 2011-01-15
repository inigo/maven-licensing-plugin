package net.surguy.maven.licensing;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.*;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Invoke from the command-line with "mvn3 licensing:touch".
 * <p/>
 * Goal which checks licenses of dependencies.
 *
 * @goal licenses
 * @phase process-sources
 */
public class LicenseCheckerMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @readonly
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private MavenProject project;

    /**
     * @parameter expression="${localRepository}" default="${maven.repo.local}"
     */
    @SuppressWarnings({"UnusedDeclaration"})
    private ArtifactRepository localRepository;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     *
     * @component role="org.apache.maven.artifact.metadata.ArtifactMetadataSource"
     * @required
     * @readonly
     */
    private MavenMetadataSource metadataSource;

    public void execute() throws MojoExecutionException {
        assert project != null : "Project is null - dependency injection is not working";

        @SuppressWarnings({"unchecked"})
        List<Dependency> directDependencies = project.getDependencies();

        Set<Artifact> artifacts = new HashSet<Artifact>();
        for (Dependency directDependency : directDependencies) {
            artifacts.add(toArtifact(directDependency));
        }

        DefaultArtifactCollector collector = new DefaultArtifactCollector();
        ResolutionListener listener = new DebugResolutionListener(new ConsoleLogger(Logger.LEVEL_DEBUG, "Resolution"));

        try {
            ArtifactResolutionResult collection = collector.collect(artifacts, project.getArtifact(), project.getManagedVersionMap(),
                    localRepository, project.getRemoteArtifactRepositories(), metadataSource, null,
                    Collections.singletonList(listener));

            @SuppressWarnings({"unchecked"})
            Set<Artifact> collectionArtifacts = collection.getArtifacts();

            LicenseExtractor licenseExtractor = new LicenseExtractor();
            for (Artifact artifact : collectionArtifacts) {
                getLog().info("Depends on " + artifact);
                File pom = pomFor(artifact);
                try {
                    List<License> licenses = licenseExtractor.retrieveLicense(pom);
                    getLog().info("   with licenses " + licenses);
                } catch (IOException e) {
                    getLog().warn("Could not read POM for artifact " + artifact + " from " + pom.getAbsolutePath()+" : "+e);
                } catch (SAXException e) {
                    getLog().warn("SAX exception reading POM for artifact "+artifact+" from "+pom.getAbsolutePath()+" : "+e);
                }
            }
        } catch (ArtifactResolutionException e) {
            getLog().error("Failed to resolve artifact with " + e, e);
        }
    }

    private DefaultArtifact toArtifact(Dependency dependency) {
        ArtifactHandler handler = new DefaultArtifactHandler();
        return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
                VersionRange.createFromVersion(dependency.getVersion()), dependency.getScope(), dependency.getType(),
                dependency.getClassifier(), handler);
    }

    private File pomFor(Artifact artifact) {
        String path = localRepository.pathOf(artifact);
        String pom = path.endsWith(".jar") ? path.substring(0, path.length() - 4) + ".pom" : path + ".pom";
        return new File(localRepository.getBasedir(), pom);
    }


}
