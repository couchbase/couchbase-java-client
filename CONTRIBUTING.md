# Contributing

We've decided to use Gerrit for our code review system, making it
easier for all of us to contribute with code and comments.

There are some prerequisites before you can start contributing, so be sure to start with the following:

  1. Visit [http://review.couchbase.org](http://review.couchbase.org) and "Register" for an account
  2. Review [our Contributor Licence Agreement (CLA)](http://review.couchbase.org/static/individual_agreement.html)
  3. Agree to CLA by visiting [http://review.couchbase.org/#/settings/agreements](http://review.couchbase.org/#/settings/agreements)
     Otherwise, you won't be able to push changes to Gerrit (instead getting a "`Upload denied for project`" message).
  4. If you do not receive an email, please contact us
  5. Have a look at current changes in review in the java client area [http://review.couchbase.org/#/q/status:open+project:couchbase-java-client,n,z](http://review.couchbase.org/#/q/status:open+project:couchbase-java-client,n,z)
  6. Join us on IRC at #libcouchbase on Freenode :-)

General information on contributing to Couchbase projects can be found on the [website](http://developer.couchbase.com/open-source-projects#how-to-contribute-code) and in the [wiki](http://www.couchbase.com/wiki/display/couchbase/Contributing+Changes).

## Building and Testing Locally
Note that to build `SNAPSHOT` versions of the `java-client` you also need to build the `core-io` package on which it depends.
Both use maven to package and install. The same process as above applies for the [core-io](https://github.com/couchbase/couchbase-jvm-core) package.

After you've checked out both projects (from github) you can build and install them as follows:

```
┌─[michael@daschlbase]─[~/code/couchbase/couchbase-jvm-core]
└──╼ mvn clean install
**SNIP**
[INFO] --- maven-install-plugin:2.4:install (default-install) @ core-io ---
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT.jar
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/dependency-reduced-pom.xml to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT.pom
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT-sources.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT-sources.jar
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT-javadoc.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT-javadoc.jar
[INFO] Installing /Users/michaelnitschinger/code/couchbase/couchbase-jvm-core/target/core-io-1.2.1-SNAPSHOT-sources.jar to /Users/michaelnitschinger/.m2/repository/com/couchbase/client/core-io/1.2.1-SNAPSHOT/core-io-1.2.1-SNAPSHOT-sources.jar
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 52.676 s
[INFO] Finished at: 2015-10-12T07:18:50+02:00
[INFO] Final Memory: 36M/337M
[INFO] ------------------------------------------------------------------------
```

Next, the exact steps apply for the  `java-client`.

Note that installing includes running the tests, which require you to run a local Couchbase Server 4.0 or later instance.
If you want to avoid building the tests over and over again, you can add the `-Dmaven.test.skip` flag to the command line.
If you only want to run the unit tests (also no server required for them), use the `-Dunit` flag (recommended over skipping the tests entirely).

## Preparing for Contribution
Gerrit needs a little bit of setup in the repository of the project you are planning to contribute to.

> **Tip**: [Here](https://www.mediawiki.org/wiki/Gerrit/Tutorial) is an extensive tutorial on how to work with git and
Gerrit. It includes explanations about a [`git-review`](https://github.com/openstack-infra/git-review) CLI tool that can
be used to work with Gerrit instead of the bare git commands described in the following sections.

You should already have created a Gerrit account and associated it with a SSH key, as well as having signed the Contributor Licence Agreement (CLA, see introduction).

You should also have performed a `git clone` of the project from Github, so we'll assume you're in the project's directory, `couchbase-java-client/`.

First you need to add a remote for Gerrit (replace `YOUR_SSH_USERNAME` with the relevant value from your ssh config):

```bash
git remote add gerrit ssh://YOUR_SSH_USERNAME@review.couchbase.org:29418/couchbase-java-client.git
```

You'll also need to install a commit hook script that will generate a new Gerrit Change-Id each time you make a brand new commit:

```bash
cd .git/hooks
wget http://review.couchbase.com/tools/hooks/commit-msg
chmod +x commit-msg
```

Keep in mind that in Gerrit (unlike Github's Pull Requests), all iterations of a particular change **must** take place in a single commit.
This is done by using `git commit --amend` (or rebasing and squashing) each time you make alterations to your change.
The discussion takes place inside of Gerrit's UI (it will provide you the URL to the change when pushing), and the history
of the change is internally maintained by Gerrit so you can actually compare each revision despite having used --amend.

## Making the Change
The previous step is a one-time configuration of the repository. The following must be done for each new contribution.

Before you start coding, you should usually open an issue in the [Couchbase bug tracker](https://issues.couchbase.com/projects/JCBC/)
(JIRA), under the relevant project: `JCBC` for Couchbase Java Client (java-client) and `JVMCBC` for Java Couchbase JVM
Core (core-io). That may avoid unnecessary effort, for example if the change you are planning to make is going to be
obsolete because of a modification we've already planned.

This will also give the change an issue number that you can reference in the commit.

To prepare a patch for the `master` branch, start from it and preferably create a new branch for your patch:

```bash
# this will be kept local, so you can use a very simple branch name
git checkout master -b myPatch
# or a fancier branch name :-)
git checkout master -b contribs/JCBC-XXX-myPatch
```

Make your changes and do the first commit, it will be issued a Change-Id:

```
git commit
```

Your preferred text editor should open for you to fill in the commit message. Note that we use a template for commit
messages that looks like this:

```txt
##IDEAL SIZE FOR COMMIT TITLE (50char)###########
#<TICKET-NR>: Commit Short Info, goes in Changelog

#Motivation
#----------
## UNCOMMENT THE ABOVE IF THE SECTION IS RELEVANT

##IDEAL SIZE FOR BODY (72char)#########################################
# A few lines about why this change is needed at all. Probably a bug
# that has shown up or why the enhancement, or new feature makes sense.

#Modifications
#-------------
## UNCOMMENT THE ABOVE IF THE SECTION IS RELEVANT

##IDEAL SIZE FOR BODY (72char)#########################################
# Explicit info about what has changed, more comments on the code that
# has changed and explicitly state breaking changes or things that
# impact users.

#Result
#------
## UNCOMMENT THE ABOVE IF THE SECTION IS RELEVANT

##IDEAL SIZE FOR BODY (72char)#########################################
# What’s the outcome of the change? For example if there is a
# performance optimization adding before/after numbers here make sense.
```

>**`TICKET-NR`** is the Jira issue id you have created, eg. `JCBC-123`.
>
> If you want to verify that the Change-Id was correctly generated, look at the end of the commit message in `git log`,
> it should appear there.

Example of a very short commit message that follows this template:

```txt
JCBC-123: Fix MadeUpClass array out of bounds

Motivation
----------
The MadeUpClass uses an array of 128 ints internally, but sometimes we
query it with an index of 256...

Modifications
-------------
Use Math.min to limit the index we query with.

Added a unit test to avoid regressions on this bug in the future.

Result
------
No more ArrayIndexOutOfBoundsException. This bug is now tested against.
```

> Notice the IDEAL SIZE lines in the template have the recommended maximum length for the section.
>
> Notice as well that in the above we uncommented the section headers and filled something in for each section.

If you want to add modifications or you come back to the change later (eg. because of discussions in Gerrit), **don't
forget to use `--amend`** (and don't modify the last lines where Gerrit metadata are appended, especially the Change-Id):

```bash
# to also edit the commit message:
git commit --amend
# if the commit message is already good:
git commit --amend --no-edit
```

Finally, to upload the change to Gerrit (as a new Changeset) or a later modification (as a new Patchset inside the
Changeset), push to the Gerrit remote's special branch:

```bash
git push gerrit HEAD:refs/for/master
```

> **Note:** As stated earlier there is a CLI tool, [`git-review`](https://github.com/openstack-infra/git-review)
> from OpenStack for dealing with Gerrit-specific commands. The above would be replaced by `git review -R`.
>
> And it generates the Change-Id, so no need to set up the commit hook manually.

Gerrit should answer with the URL to your Changeset, where you can call for reviewers (for the Java SDK, `Michael Nitschinger`).

To mark a changeset as ready for review (you are confident the change is complete with code and tests, and you have
executed all unit tests and preferably all integration tests), you can use the "Reply..." button, top right and give
"`Verified`" a note of +1.

Reviewers will be notified and will look at your change, replying with a "`Code-Review`" mark between -2 and +2.
If <= 0, there should be comments visible in the bottom list, starting a discussion to improve the change until a later
patchset can receive a `Code-Review +2` and be merged in.

## Troubleshooting
### "Upload denied for project"
If you get the following error while attempting to push your first change to Gerrit:
```
fatal: Upload denied for project 'couchbase-java-client'
fatal: Could not read from remote repository.
Please make sure you have the correct access rights
and the repository exists.
```

Make sure that you have accepted the CLA as described in step 3. of the intro. Also please check your ssh configuration.

This applies both when using bare git (`git push gerrit HEAD:refs/for/master`) or git-review ( `git review -R`).

## Final Note
Finally, feel free to reach out to the maintainers over the forums, IRC or email ([sdk_dev@couchbase.com](mailto:sdk_dev@couchbase.com))
if you have further questions on contributing or get stuck along the way. **We love contributions and want to help you
get your change over the finish line - and you mentioned in the release notes!**
