## Zulip notifier plugin for Jenkins

This plugin sends [messages](#zulip-send) and [notifications](#zulip-notification) of build statuses to Zulip.

It began its life as a fork of the [Jenkins Campfire plugin](https://github.com/jenkinsci/campfire-plugin).

## Global configuration

The Zulip server is configured globally for the whole Jenkins instance.

Minimal configuration is:
* **Zulip Url** = The Url of your Zulip installation visible from Jenkins instance.
* **Zulip User Email** = E-mail of Zulip bot that will be used to send messages to streams.
* **Zulip API key** = The API key of bot that will be used to send messages to streams.

Other attributes are optional:
* **Default Stream Name** = The stream messages will be sent to by default.
You can override this setting per project, but be sure not to leave it blank in both places
or your messages will fail to send.
You can use build variables in default stream name.
* **Default Topic Name** = The topic messages will be sent to by default.
You can override this setting per project. If you leave it blank at both places,
Jenkins job name will be used as a topic instead.
You can use build variables in default topic name.
* **Enable Smart Notification** = If enabled, successful build notification will be sent out only if
one of following criteria is met:
    * There was no previous build
    * The current build failed
    * The current build is unstable (failed tests)
    * Previous build failed
    
    In another words, the notifications will not be sent for continuously successful builds.
* **Use full job path in default topic name** = Determines how the topic name is constructed when **Default topic name** is blank:
    * When disabled, only the build run direct parent name is used.
    * When enabled, all parent names (e.g. folders) are used.
    
    It is recommended to have this option on, especially if you use projects with multiple layers (e.g. multi-branch pipeline).
* **Use full job path in notification message** = Determines how the build notification message is constructed.
    * When disabled, only the build run direct parent name is used.
    * When enabled, all parent names (e.g. folders) are used.
    
    It is recommended to have this option on, especially if you use projects with multiple layers (e.g. multi-branch pipeline).
* **Jenkins URL** = :warning: This parameter is kept just for the sake of backward compatibility.:warning:
Instead of setting this, configure Jenkins URL in "Manage Jenkins" > "Configure System" > "Jenkins Location" > "Jenkins URL" 

![Global Settings](docs/global-settings.png)

## Zulip Notification

Zulip notification is a post build action step, that posts notification about build statuses to Zulip Streams.
The step allows you to configure two optional parameters:
* **Stream** = The stream messages will be sent to for this job. Will override default stream from global settings.
Be sure not to leave it blank in both places.
You can use build variables in stream name.
* **Topic** = The topic messages will be sent to for this job. Will override default topic from global settings.
If blank in both places, job name will be used as topic.
You can use build variables in topic name.
* **Smart Notification** = Allows you override global smart notification setting and enable / disabled
smart notification for this job. The job uses global smart notification setting by default. 

#### Freestyle project 

For freestyle project, simply select a post build action from the dropdown
and optionally configure destination stream and topic.

![Zulip Notification](docs/zulip-notification.png)

#### Scripted Pipeline

Scripted pipeline have no concept of post build actions, but you can still use the ```zulipNotification```
step in the try/catch or preferably using the ```catchError``` step.

```jenkins
node {
    catchError {
        // ... Your build stages ...
    }
    zulipNotification stream: 'coolproject', topic: 'jenkins', smartNotification: 'enabled'
}
```

#### Declarative Pipeline

In declarative pipeline, simply use the ```zulipNotification``` step inside your post actions.

```jenkins
pipeline {
    agent any
    stages {
        // ... Your build stages ...
    }
    post {
        always {
            zulipNotification()
        }
    }
}
```


## Zulip Send

Zulip send is a build step, that allows you to post arbitrary messages to Zulip stream throughout the build process.
You can use this e.g. to notify Zulip that build has started or about various phases the build goes through.
The step allows you to configure:
* **Stream** = Optional stream the message will be sent to for this job. Will override default stream from global settings.
Be sure not to leave it blank in both places.
You can use build variables in the stream name.
* **Topic** = Optional topic the message will be sent to for this job. Will override default topic from global settings.
If blank in both places, job name will be used as topic.
You can use build variables in the topic name.
* **Message** = The message that will sent out. You can use build variables inside the message.

#### Freestyle project

For freestyle project, simply select a build action from the dropdown and fill in desired message.
Optionally configure destination stream and topic.

![Zulip Send](docs/zulip-send.png)

#### Scripted Pipeline

In scripted pipeline, simply use the ```zulipSend``` step in any stage of the build.

```jenkins
node {
    stage('Prepare') {
        zulipSend message: 'Started build #${BUILD_NUMBER} of project ${JOB_NAME}...'
        // ... Perhaps SCM checkout here ...
    }
    // ... Other build stages ...
}
```

#### Declarative Pipeline

In declarative pipeline, simply use the ```zulipSend``` step in any stage of the build.

```jenkins
pipeline {
    agent any
    stages {
        stage('Prepare') {
            steps {
                zulipSend message: 'Started build #${BUILD_NUMBER} of project ${JOB_NAME}...'
                // ... Perhaps SCM checkout here ...
            }
        }
        // ... Other build stages ...
    }
}
```

## Job DSL

There is no explicit support for the Job DSL Plugin, but Zulip Send
step and Zulip Notification can be configured via [dynamically
generated DSLs](https://github.com/jenkinsci/job-dsl-plugin/wiki/Dynamic-DSL).

Example DSL creating a freestyle job using both Zulip Send and Zulip Notification:
```jenkins

job('DSL-Freestyle') {
  steps {
    zulipSend {
      message('Hello via job DSL!')
      stream('dslproject')
      topic('jenkins')
    }
  }
  publishers {
    zulipNotification {
      stream('dslproject')
      topic('jenkins')
    }
  }
}
```

## Troubleshooting

#### No such DSL method

If you're using 1.x version of Jenkins and 1.x version of workflow job plugin (previous name for the pipeline jobs)
you will encounter exceptions like ```java.lang.NoSuchMethodError: No such DSL method zulipSend found among [...]```.

In that case, you will have to use the meta-step instead. Simply replace:

```jenkins
zulipSend message: 'Test'
```
with
```jenkins
step([$class: 'ZulipSendStep', message: 'Test'])
```

and

```jenkins
zulipNotification()
```

with

```jenkins
step([$class: 'ZulipNotifier'])
```