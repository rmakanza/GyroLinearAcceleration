GyroLinearAcceleration
======================

Android linear acceleration with a gyroscope and acceleration sensor fusion via complimentary filter. Gyro Linear Acceleration is intended to provide code examples and a working application for developers, students and hobbyists who are interested in sensor fusions that are capable of measuring linear acceleration. While code is intended for Android devices, the jist of the algorithm can be applied to any language/hardware configuration.

The linear acceleration of an object is calculated as the acceleration of the device minus the force of the earth's gravitational field ( the tilt of the device). Gyro Linear Acceleration uses a complimentary filter to fuse the acceleration sensor and gyroscope sensor together to provide a measurement of the devices linear acceleration. The acceleration sensor alone is not capable of distinguishing true linear acceleration from tilt, or gravity. The gyroscope sensor is used to find the tilt of the device. The tilt angle of the device can then be used to calculate the gravity component of the acceleration that can then be subtracted from the acceleration to find the linear acceleration.

Most people will find that the end-result of this implementation is that, while linear acceleration can be measured while the device is static (not accelerating), linear acceleration cannot be accurately measured while the device is actually under linear acceleration. This is because the complementary filter, which is used to compensate for the drift of the gyroscope, begins to assume the acceleration of the device is actually tilt, skewing the rotation measurements from the gyroscope.

Related to the linear acceleration problem is that the gyroscope sensor can easily drift out of rotation with the device when it is experiencing vibrations or rapid rotations, even with the help of an acceleration sensor. If you modify the complementary filter to quickly compensate for gyroscope drift with the acceleration sensor, you increase the problem of the complimentary filter confusing linear acceleration for tilt when the device is actually accelerating. 

Gyro Linear Acceleration may work well for determining linear acceleration for a static device, for instance, that moves a character or vehicle in a game by tilting the device. Gyro Linear Acceleration will not work well for determining the linear acceleration of a vehicle or other object that actually accelerates the device.

Gyro Linear Acceleration will plot the output of the sensor fusion in real-time and will also log the data to an external .CSV file that can be viewed at a later time on any spreadsheet application.

