How to report a bug
-------------------
- Before anything else, please make sure you are on the latest version, the bug you are experiencing may have been fixed already!
- Use the search function to see if someone else has already submitted the same bug report.
- Try to describe the problem with as much detail as possible.
- Some bugs may only occur on certain devices or versions of Android. Please add information about your device and the version of Android that is running on it (you can look these up under `Settings → About Phone`), as well as which version of FocusPodcast you are using.
- If the bug only seems to occur with a certain podcast, please include the URL of that podcast.
- If possible, add instructions on how to reproduce the bug.
- If possible, add a logfile to your post. This is especially useful if the bug makes the application crash. FocusPodcast has an `report bug` feature for this.
- Usually, you can take a screenshot of your smartphone by pressing *Power* + *Volume down* for a few seconds.
- Please use the following **[template](https://github.com/allentown521/FocusPodcast/issues/new?assignees=&labels=Type%3A+Possible+bug&template=bug_report.yml)**.


How to submit a feature request
-------------------------------
- Make sure you are using the latest version of FocusPodcast. Perhaps the feature you are looking for has already been implemented.
- Use the search function to see if someone else has already submitted the same feature request. If there is another request already, please upvote the first post instead of commenting something like "I also want this".
- To make it easier for us to keep track of requests, please only make one feature request per issue.
- Give a brief explanation about the problem that may currently exist and how your requested feature solves this problem.
- Try to be as specific as possible. Please not only explain what the feature does, but also how. If your request is about (or includes) changing or extending the UI, describe what the UI would look like and how the user would interact with it.
- Please use the following **[template](https://github.com/allentown521/FocusPodcast/issues/new?assignees=&labels=&template=feature_request.yml)**.



Submit a pull request
---------------------
- Before you work on the code
    - Make sure that there is an issue *without* the `Needs: Triage` or `Needs: Decision` label for the feature you want to implement or bug you want to fix.
    - Add a comment to the issue so that other people know that you are working on it.
        - You don't need to ask for permission to work on something, just indicate that you are doing so.
- Fork the repository
- Create a new branch for your contribution
    - This makes opening possible additional pull requests easier.
    - As a base, use the `develop` branch.
        - Almost all changes of FocusPodcast are done on the `develop` branch. If a new version of FocusPodcast is released, the `develop` branch is merged into `master`. As a result, the `master` branch probably doesn't contain the latest changes when you are reading this. Otherwise, there might be a lot of merge-conflicts when merging your changes into `develop` and therefore it might take longer to review your pull-request.
- Get coding :)
    - If possible, add unit tests for your pull request and make sure that they pass.
    - Please do not upgrade dependencies or build tools unless you have a good reason for it. Doing so can easily introduce bugs that are hard to track down.
- Open the PR
    - Mention the corresponding issue in the pull request text, so that it can be closed once your pull request has been merged. If you use [special keywords](https://docs.github.com/en/issues/tracking-your-work-with-issues/linking-a-pull-request-to-an-issue), GitHub will close the issue(s) automatically.


Building From Source
--------------------------
1. Fork this repository
1. Download Latest version Android Studio
1. Download FocusPodcast
    1. Option A: Using the git command line (recommended)
        1. Use `git clone <url>` with the remote url of your forked repo.
           The FocusPodcast repo contains a large submodule with app store metadata like screenshots.
           You **do not need that** for normal development.
        1. In Android Studio: File » New » Project from existing sources
    1. Option B: From Android Studio
        1. File » New » Project from version control
        1. Enter the remote url of the forked repo
2. Use Java 17 in Android Studio Settings - Gradle
1. Wait for a long time until all progress bars go away
1. Press the Play button
