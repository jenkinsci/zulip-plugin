# CHANGELOG

------

# 2.1.1 Latest
------

## What's Changed

* chore: fix jenkins pipeline by [@butchyyyy](https://github.com/butchyyyy) in [#41](https://github.com/jenkinsci/zulip-plugin/pull/41)
* fix: java 17 authentication by [@butchyyyy](https://github.com/butchyyyy) in [#42](https://github.com/jenkinsci/zulip-plugin/pull/42)

Full Changelog: [2.1.0...2.1.1](https://github.com/jenkinsci/zulip-plugin/compare/2.1.0...2.1.1)

# 2.1.0 
------

* Following global config options are now enabled by default for new plugin installations:
    * Use full job path in default topic name
    * Use full job path in notification message
    * Enable smart notification
* Added more verbose logging around proxy setup and authentication

Full Changelog: [2.0.0...2.1.0](https://github.com/jenkinsci/zulip-plugin/compare/2.0.0...2.1.0)


# 2.0.0
------

## What's Changed

* feat: upgrade jenkins and java by @butchyyyy in #38
* Minimal required Jenkins version is increased to 2.387.3 (LTS)
* Minimal required Java version increased to 11 (also required by Jenkins)
* Apache commons HttpClient replaced by Java net HttpClient
* Remove unnecessary plugin dependencies
* Replace Powermock static and constructor mocking with Mockito capabilities.

Full Changelog: [1.3.0...2.0.0](https://github.com/jenkinsci/zulip-plugin/compare/1.3.0...2.0.0)


# 1.3.0
------

## Features

* Introduces new global settings to enable displaying full job name in build result messages and as topic name. This is especially useful for multi branch pipeline jobs. The original behaviour only displayed project name === branch. This was not very useful information when several pipelines posted into same stream. After enabling the new settings, full name including the multi branch pipeline folders will be displayed [(#31)](https://github.com/jenkinsci/zulip-plugin/issues/31)


# 1.2.1
------

* [#26](https://github.com/jenkinsci/zulip-plugin/issues/26) Replace :x: failed build emoji with :cross_mark:


# 1.2.0
------

## Features

* New zulip notifier step smart notification option, that allows users to configure smart notifications per job (step). Previously smart notification setting was only possible to set for the whole Jenkins instance in global settings.

## Bugfixes

* Fix parallel builds being blocked by zulip notifier step


# 1.1.1
------

* [SECURITY-1621] Store Zulip API key from global configuration as Secret.


# 1.1.0
------

* Expansion of Jenkins build variables in stream and topic name.


# 1.0.3
------

* Improved logging when message to Zulip fails to send (Whole IOException stack trace is logged instead of just message).
* Fix of Jenkins' proxy configuration usage.
    * Abide to no proxy host settings
    * Use username and password credentials


# 1.0.2
------

* Fix of regression bug from 1.0.1 release that made it impossible to disable smart notifications in global configuration.


# 1.0.1
------

* Fix of Zulip url global configuration coliding with another plugin's (e.g. repository-connector-plugin) settings making it impossible to configure Zulip url


# 1.0.0
------

* Initial plugin release


# RELEASES

(https://github.com/jenkinsci/zulip-plugin/releases)