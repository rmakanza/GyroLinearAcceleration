GyroLinearAcceleration
======================

![](http://www.kircherelectronics.com/bundles/keweb/css/images/gyro_linear_acceleration_phone_graphic.png?raw=true)
 
Gyro Linear Acceleration is intended to provide code examples and a working application for developers, students and hobbyists who are interested in sensor fusions that are capable of measuring linear acceleration. While code is intended for Android devices, the jist of the algorithm can be applied to any language/hardware configuration.

The linear acceleration of an object is calculated as the acceleration of the device minus the force of the earth's gravitational field ( the tilt of the device). Gyro Linear Acceleration uses a complimentary filter to fuse the acceleration sensor and gyroscope sensor together to provide a measurement of the devices linear acceleration. The acceleration sensor alone is not capable of distinguishing true linear acceleration from tilt, or gravity. The gyroscope sensor is used to find the tilt of the device. The tilt angle of the device can then be used to calculate the gravity component of the acceleration that can then be subtracted from the acceleration to find the linear acceleration.

Most people will find that the end-result of this implementation is that, while linear acceleration can be measured while the device is static (not accelerating), linear acceleration cannot be accurately measured while the device is actually under linear acceleration. This is because the complementary filter, which is used to compensate for the drift of the gyroscope, begins to assume the acceleration of the device is actually tilt, skewing the rotation measurements from the gyroscope. This problem can also be seen in most Android implementations of Sensor.TYPE_LINEAR_ACCELERATION. You can explore the issue further with [Android Linear Acceleration](https://github.com/KEOpenSource/AndroidLinearAcceleration). The problem can be mitigated to a large extent by using concepts from [Simple Linear Acceleration](https://github.com/KEOpenSource/SimpleLinearAcceleration).

Related to the linear acceleration problem is that the gyroscope sensor can easily drift out of rotation with the device when it is experiencing vibrations or rapid rotations, even with the help of an acceleration sensor. If you modify the complementary filter to quickly compensate for gyroscope drift with the acceleration sensor, you increase the problem of the complimentary filter confusing linear acceleration for tilt when the device is actually accelerating. Applying concepts from [Fused Gyroscope Explorer](https://github.com/KEOpenSource/FusedGyroscopeExplorer) can help to increase the reliability of the gyroscope.

Gyro Linear Acceleration may work well for determining linear acceleration for a static device, for instance, that moves a character or vehicle in a game by tilting the device. Gyro Linear Acceleration will not work well for determining the linear acceleration of a vehicle or other object that actually accelerates the device. However, with careful consideration for the application, a few modifcations to Gyro Linear Acceleration can produce the desired results for most applications.  Most of these considerations have been implemented in [Fused Linear Acceleration](https://github.com/KEOpenSource/FusedLinearAcceleration) which provides a reliable estimation of linear acceleration under most conditions.

Features:
* Plot linear acceleration in real-time
* Log linear acceleration to an external .CSV file
* Compare different implementations of linear acceleration to Gyro Linear Acceleration

Useful Links:

* [Gyro Linear Acceleration Homepage](http://www.kircherelectronics.com/gyrolinearacceleration/gyrolinearacceleration)
* [Gyro Linear Acceleration Community](http://kircherelectronics.com/forum/viewforum.php?f=11)
* [Gyro Linear Acceleration Blog Article](http://www.kircherelectronics.com/blog/index.php/11-android/sensors/17-gyroscope-linear-acceleration)
* [Download Gyro Linear Acceleration from Google Play](https://play.google.com/store/apps/details?id=com.kircherelectronics.gyrolinearacceleration)

Written by [Kircher Electronics](https://www.kircherelectronics.com)
