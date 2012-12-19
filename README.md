This is simple example of how to create a plugin for Stash that will trigger a URL webhook in Jenkins.

When running Stash add the following to your command line arguments:

    -Dwebhook.url=http://localhost:8080/jenkins/git/notifyCommit?url=$URL&branches=$BRANCHES

Where $URL will be replaced with the HTTP url of the repository, and $BRANCHES contains a comma separated list of branches.