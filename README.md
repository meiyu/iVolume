## 代码结构

iVolume\app\src\main\java\com\ivolume\ivolume目录下为代码文件，有用的代码功能分别为：

- InfoActivity.java：介绍页面。

- MainActivity.java：主页面，同时控制页面的跳转，通知发送，弹出权限申请等。

- MainService.java：主服务，负责持续进行多种情境的监听，包括当前地点、前台app、环境噪音、接入设备。同时在情境变化时触发自动调节机制，还可以处理、记录用户手动调节音量的操作。

- NoiseDetector.java：监测背景噪音的大小。

- Questionnaire_Activity.java：问卷页面，反馈问卷填写结果。

- SettingsActivity.java：设置页面，可以选择屏蔽某些情境下的自动音量调节。

- VolumeUpdater.java：自动调节模块，含有音量调节算法，根据情境和历史信息调整媒体音量。同时含有存储单元记录用户不同情境下的历史音量信息，并跟据反馈机制传来的信息对其进行修改。还可以打印运行日志。

iVolume/app/src/main/res/layout目录下为布局文件，分别为：

- activity_info.xml：介绍页面。
- activity_main.xml：主页面。
- activity_questionaire.xml：问卷页面。
- activity_settings.xml：设置页面。

iVolume/app/src/main/res/drawable目录下为图片资源，包括iVolume的logo、服务开关按钮、噪音检测按钮等。

## 机型

测试机型：

- 1.HUAWEI NOVA7

- 2.HUAWEI mate40 pro

## 项目运行环境

安卓系统 android api>=31 

## 项目运行方法

​		第一次进入app，会弹出无障碍权限的提示，选择已安装的服务中的ivolume打开，同时第一次进入的时候会提示打开位置权限和麦克风权限。某些手机可能默认会将通知拦截，也需要到设置中将应用->ivolume的通知权限打开。此后每次退出app重新进入时还需要将无障碍权限再次打开。

​		进入主页面后，可以点击主界面右上方的图标进入设置页面，选择屏蔽某些前台app、地点或接入设备；也可以点击主界面左上方的图标进入介绍页面，阅读ivolume的简介。

​		进入app主页面后，点击下方的噪音校准按钮，在安静的地方进行噪音校准，然后点击上方图标即可开始服务，此时图标变为蓝色。随后在目前支持的情境下，ivolume在每次切换情境时会进行自动音量调节。

​		目前支持的前台app有：腾讯会议、bilibili、网易云音乐、乐动力。每次在上述app内手动按音量键调节音量后，会弹出反馈问卷，点击问卷通知，进入问卷页面，根据需要进行填写，问卷的结果会用于调整ivolume的音量调节算法。

​		需要注意，由于每个人的音量习惯不同，且音量调节行为的发生频率不高，因此需要通过一段时间app才能适应音量习惯，做出比较合理的音量调整。
