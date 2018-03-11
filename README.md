# LanTV

## About

This is an experimental Android app for getting [CCTV Channels](http://tv.cctv.com/live/) to play on the [MoonBox M3S](http://www.moonbox.hk/m3s.html), as there seems to be no alternative app that supports a remote and the same variety of channels.

## How to use

After running it will connect to CCTV13.  The channel can be changed by using up/down on your remote or keyboard.

## Issues

- Error messages could be more informative.
- The previous channel is forgotten across restarts.
- Resolving a channel is slow (it's scraping the CCTV website after all), but these URLs could be cached.  The video URLs seem to remain valid for many hours.
- No touch interface.
- Orientation changes trigger a reconnect.
- No tests.
