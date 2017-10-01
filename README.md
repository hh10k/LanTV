# LanTV

## About

This is an experimental Android app for getting [CCTV Channels](http://tv.cctv.com/live/) to play on the [MoonBox M3S](http://www.moonbox.hk/m3s.html), as there seems to be no alternative app that supports a remote.  The original MoonBox apps supported these channels, but those apps no longer work.

## How to use

After running it will connect to CCTV13.  The channel can be changed by using up/down on your remote.

## Issues

- Video stutters ever 10 or so seconds.  LibVLC reports that there is a time sync problem but it's not obviously related to network or performance limitations of the MoonBox.  It works flawlessly in an emulator, so it's likely there is a bottleneck somewhere.
- Status sometimes disappears (behind video?).
- Error messages are not informative.
- The previous channel is forgotten across restarts.
- Resolving a channel is slow (it's scraping the CCTV website after all), but these URLs could be cached.  The video URLs seem to remain valid for many hours.
- No way to change the channel without a remote.
- Orientation changes trigger a reconnect.
- No tests.

## Building LibVLC

LibVLC must be build separately from this repository.

1) Look at [the wiki](https://wiki.videolan.org/AndroidCompile/), it's mostly correct.  I used Windows 10 Ubuntu shell as a build environment.
2) Install an Android SDK and NDK.  NDKs newer than r14b do not support the target platform of VLC.
3) Get libvlc
    * `git clone https://code.videolan.org/videolan/vlc-android.git`
    * `cd vlc-android`
    * `git checkout 2.1.17`
4) Build, and get artifacts.  Your first build *must* use `--release` otherwise it will not check out the correct commit of VLC!
    * `compile.sh --release -a armeabi-v7a`
    * Keep `./libvlc/build/outputs/aar/libvlc-3.0.0-null.aar`
    * `compile.sh --release -a x86`
    * Keep `./libvlc/build/outputs/aar/libvlc-3.0.0-null.aar`
5) Combine `libvlc-x86.aar` and `libvlc-armeabi-v7a.aar` by adding the libs from the former into the latter.
6) Put the combined `libvlc.aar` into `app/libs/`

## Licence

Note that while this repository is [MIT licensed](./LICENSE.md), the Android LibVLC library is GPL, so the application that is built by this is also GPL.