package net.surguy.maven.licensing

import org.apache.maven.artifact.Artifact
import scala.collection.JavaConversions._

import org.apache.maven.artifact.DefaultArtifact
import org.apache.maven.artifact.handler.DefaultArtifactHandler
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.artifact.resolver._
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter
import org.apache.maven.artifact.versioning.VersionRange
import org.apache.maven.model.Dependency
import org.apache.maven.project.MavenProject
import org.apache.maven.project.artifact.MavenMetadataSource
import org.apache.maven.shared.dependency.tree.DependencyNode
import org.apache.maven.shared.dependency.tree.DependencyTreeResolutionListener
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor
import org.codehaus.plexus.logging.Logger
import org.codehaus.plexus.logging.console.ConsoleLogger
import java.io.File
import org.apache.maven.plugin.logging.Log

/**
 * Process the dependencies for a project, and identify the licenses for each.
 *
 * @author Inigo Surguy
 * @created 16/01/2011 22:14
 */
class LicenseChecker(project: MavenProject, localRepository: ArtifactRepository, metadataSource: MavenMetadataSource, log: Log) {
  val licenseExtractor = new LicenseExtractor()

  def execute() {
    val artifacts:Set[Artifact] = project.getDependencies.map(d => toArtifact(d.asInstanceOf[Dependency])).collect{case a: Artifact => a}.toSet

    val collector = new DefaultArtifactCollector()
    val listener = new DependencyTreeResolutionListener(new ConsoleLogger(Logger.LEVEL_WARN, "Resolution"))
    collector.collect(artifacts, project.getArtifact(), project.getManagedVersionMap(),
      localRepository, project.getRemoteArtifactRepositories(), metadataSource, new ScopeArtifactFilter("compile"),
      List(listener))

    listener.getRootNode.accept(new DependencyNodeVisitor() {
      var depth = 0

      def visit(node: DependencyNode):Boolean = {
        depth = depth + 1
        node match {
          case node if node.getState != DependencyNode.INCLUDED => false
          case node if node.getArtifact.getScope==null || node.getArtifact.getScope=="compile" =>
            val artifact = node.getArtifact()
            val licenses = getLicenses(artifact)
            log.info( ("  " * depth) + artifact + licenses )
            true
          case _ => false
        }
      }

      def endVisit(node: DependencyNode) = {
        depth = depth - 1
        true
      }
    });
  }

  private def getLicenses(artifact: Artifact) = licenseExtractor.retrieveLicense(pomFor(artifact))

  private def toArtifact(dependency: Dependency): Artifact =
    new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
       VersionRange.createFromVersion(dependency.getVersion()), dependency.getScope(), dependency.getType(),
       dependency.getClassifier(), new DefaultArtifactHandler())

  private def pomFor(artifact: Artifact) = {
    val path = localRepository.pathOf(artifact)
    val pom = if (path.endsWith(".jar")) path.substring(0, path.length() - 4) + ".pom" else path + ".pom"
    new File(localRepository.getBasedir(), pom)
  }
}

/**
 * Represents a software license.
 *
 * @author Inigo Surguy
 * @created 16/01/2011 18:25
 */
case class License(name: String, url: String)