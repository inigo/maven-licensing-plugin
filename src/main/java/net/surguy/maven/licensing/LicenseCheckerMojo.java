package net.surguy.maven.licensing;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.*;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.MavenMetadataSource;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeResolutionListener;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.codehaus.plexus.util.StringUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maven plugin to display the licenses of all dependencies, if they are specified in the POM.
 * <p/>
 * Invoke from the command-line with "mvn licensing:licenses".
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
        DependencyTreeResolutionListener listener = new DependencyTreeResolutionListener(new ConsoleLogger(Logger.LEVEL_DEBUG, "Resolution"));
        try {
            collector.collect(artifacts, project.getArtifact(), project.getManagedVersionMap(),
                    localRepository, project.getRemoteArtifactRepositories(), metadataSource, new ScopeArtifactFilter("compile"),
                    Collections.singletonList(listener));
        } catch (ArtifactResolutionException e) {
            getLog().error("Failed to resolve artifact with " + e, e);
        }

        final Log log = getLog();
        DependencyNode rootNode = listener.getRootNode();
        rootNode.accept(new DependencyNodeVisitor() {
            private int depth = 0;
            public boolean visit(DependencyNode node) {
                depth++;
                if (node.getState()!=DependencyNode.INCLUDED) return false;
                String scope = node.getArtifact().getScope();
                // Scope is null for the root dependency
                if (scope == null || scope.equals("compile")) {
                    Artifact artifact = node.getArtifact();
                    List<License> licenses = getLicenses(artifact);
                    log.info(StringUtils.repeat("  ", depth) + artifact + licenses);
                    return true;
                } else {
                    return false;
                }
            }
            public boolean endVisit(DependencyNode node) {
                depth--;
                return true;
            }
        });
    }

    private List<License> getLicenses(Artifact artifact) {
        LicenseExtractor licenseExtractor = new LicenseExtractor();
        List<License> licenses = Collections.emptyList();
        File pom = pomFor(artifact);
        try {
            licenses = licenseExtractor.retrieveLicense(pom);
        } catch (IOException e) {
            getLog().warn("Could not read POM for artifact " + artifact + " from " + pom.getAbsolutePath() + " : " + e);
        } catch (SAXException e) {
            getLog().warn("SAX exception reading POM for artifact " + artifact + " from " + pom.getAbsolutePath() + " : " + e);
        }
        return licenses;
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
