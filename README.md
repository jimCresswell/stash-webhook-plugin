This is simple example of how to create a plugin for Stash that will trigger a URL webhook in Jenkins.

When running Stash you can enable the webhook for a specific repository via the
[Repository Hook](https://confluence.atlassian.com/display/STASH/Using+repository+hooks) configuration.

Where $URL will be replaced with the HTTP url of the repository, and $BRANCHES contains a comma separated list of branches.

To build this plugin use the standard Atlassian Plugin SDK and/or run the following (*must* be Maven 2.1.0):

    mvn install

And upload `target/stash-webhook-plugin-2.0.0-SNAPSHOT.jar` to Stash.
