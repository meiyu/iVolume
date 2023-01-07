## 代码结构

iVolume\app\src\main\java\com\ivolume\ivolume目录下为代码文件，有用的代码功能分别为：

MainActivity.java：主页面，同时控制页面的跳转，通知发送，弹出权限申请等

MainService.java：主服务，负责监测（或调用接口）位置信息、前台应用信息、背景声音信息、耳机信息等，同时调用自动调节音量的程序、记录用户调节音量的操作

NoiseDetector.java：监测背景声音的大小的程序。

Questionnaire_Activity.java：问卷页面

SettingsActivity.java：设置页面

VolumeUpdater.java：自动调节音量的程序，根据上述信息调整音量，记录用户调节以后的表项，打印运行日志

## 机型

测试机型：

- 1.HUAWEI NOVA7

- 2.HUAWEI mate40 pro

## 项目运行环境

安卓系统 android api>=31 

## 项目运行方法

第一次进入app，会弹出无障碍权限的提示，选择已安装的服务中的ivolume打开，同时第一次进入的时候会提示打开位置权限和麦克风权限。某些手机可能默认会将通知拦截，也需要到设置中将通知权限打开。此后每次退出app重新进入时还需要将无障碍权限再次打开。

进入app口点击下方噪音校准，在安静的地方矫正背景噪音值，然后点击上方图标即可开始服务，进入设置可以控制提供服务的前台app、地点、接入设备。

目前支持的前台app有：腾讯会议、bilibili、网易云音乐、乐动力






