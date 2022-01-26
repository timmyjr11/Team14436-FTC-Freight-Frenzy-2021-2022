package org.firstinspires.ftc.teamcode.drive;

import com.acmerobotics.roadrunner.geometry.Pose2d;

public class PoseStorage {
    public static Pose2d telePower = new Pose2d(0,0, Math.toRadians(0));

    public static Pose2d rightAutoRed = new Pose2d(9.5, -62.5, -90);
    public static Pose2d leftAutoRed = new Pose2d(-38, -62.5, -90);

    public static Pose2d rightAutoBlue = new Pose2d(-54, 32.25, 90);
    public static Pose2d leftAutoBlue = new Pose2d(-54, 9, 90);
}
