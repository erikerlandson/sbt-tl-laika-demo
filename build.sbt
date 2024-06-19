// If you make changes to this build configuration, then also run:
// sbt githubWorkflowGenerate
// and check in the updates to github workflow yamls

// base version for assessing MIMA
ThisBuild / tlBaseVersion := "0.1"

// publish settings
// artifacts now publish to s01.oss.sonatype.org, per:
// https://github.com/erikerlandson/coulomb/issues/500
//
// The sonatype publishing depends on 3 github action secrets,
// which are explained here:
// https://typelevel.org/sbt-typelevel/secrets.html
//
// On linux, you need to give a '-w 0' flag for base64:
// gpg --list-secret-keys
// gpg --armor --export-secret-keys $KEY_ID | base64 -w 0
//
// my gpg key is the one with the comment:
// "SBT Sonatype (publish sbt to sonatype)"
//
// As of June 2024, you also need to use the "api token"
// for sonatype, not the legacy user name and password,
// instructions here:
// https://typelevel.org/sbt-typelevel/secrets.html#sonatype-credentials
ThisBuild / developers += tlGitHubDev("erikerlandson", "Erik Erlandson")
ThisBuild / organization := "com.manyangled"
ThisBuild / organizationName := "Erik Erlandson"
ThisBuild / licenses := List(
    "Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / startYear := Some(2024)

// ci settings
ThisBuild / tlCiReleaseBranches := Seq("main")
ThisBuild / tlSitePublishBranch := Some("main")
// use jdk 17 in ci builds
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("17"))

ThisBuild / resolvers ++= Resolver.sonatypeOssRepos("snapshots")
ThisBuild / crossScalaVersions := Seq("3.4.2")

// run tests sequentially for easier failure debugging
Test / parallelExecution := false

// throwing '-java-output-version 8' is crashing the compiler
ThisBuild / tlJdkRelease := None

// At least for now, I'd like any -W warnings to not crash the build
ThisBuild / tlFatalWarnings := false

lazy val root = tlCrossRootProject
    .aggregate(
        core
    )

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(CrossType.Pure)
    .in(file("core"))
    .settings(name := "sbttllaika-core")

// a target for rolling up all subproject deps: a convenient
// way to get a repl that has access to all subprojects
// sbt all/console
lazy val all = project
    .in(file("all")) // sbt will create this - it is unused
    .dependsOn(
        core.jvm
    ) // scala repl only needs JVMPlatform subproj builds
    .settings(name := "sbttllaika-all")
    .enablePlugins(NoPublishPlugin) // don't publish

// a published artifact aggregating API docs for viewing at javadoc.io
// build and view scaladocs locally:
// sbt unidocs/doc
// view at:  file:///your/path/to/coulomb/unidocs/target/scala-3.1.2/unidoc/index.html
// serve locally:
// python3 -m http.server -d unidocs/target/scala-3.1.2/unidoc/
lazy val unidocs = project
    .in(file("unidocs")) // sbt will create this
    .settings(name := "sbttllaika-docs") // the name of the artifact
    .enablePlugins(TypelevelUnidocPlugin) // enable Unidoc + publishing

// https://typelevel.org/sbt-typelevel/site.html
// sbt site/tlSitePreview
// http://localhost:4242
import laika.config.{Version, Versions}
import laika.theme.ThemeProvider
import laika.helium.Helium

val siteVersions = Versions
    .forCurrentVersion(Version("0.1.0", "0.1.0").setCanonical)
    .withNewerVersions(
        Version("0.2.0-RC1", "0.2.0-RC1").withLabel("RC")
    )

val siteTheme: ThemeProvider = Helium.defaults.site.versions(siteVersions).build

lazy val site = project
    .in(file("docs"))
    .dependsOn(
        core.jvm
    )
    .enablePlugins(TypelevelSitePlugin)
    .settings(
        // turn off the new -W warnings in mdoc scala compilations
        // at least until I can get a better handle on how to work with them
        Compile / scalacOptions ~= (_.filterNot { x => x.startsWith("-W") })
    )
    .settings(
        laikaTheme := siteTheme
    )
