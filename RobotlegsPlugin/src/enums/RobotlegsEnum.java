package enums;

/**
 * User: John Lindquist
 * Date: 6/24/11
 * Time: 4:11 AM
 */
public enum RobotlegsEnum
{
    VIEW("flash.display.DisplayObjectContainer", "mapView", 1),
    MEDIATOR("org.robotlegs.core.IMediator", "mapView", 0),
    EVENT("flash.events.Event", "mapEvent", 1),
    COMMAND("org.robotlegs.mvcs.Command", "mapEvent", 0),
    SIGNAL("org.osflash.signals.Signal", "mapSignalClass", 1),
    SIGNALCOMMAND("org.robotlegs.mvcs.SignalCommand", "mapSignalClass", 0);



    private String classQName;
    private String mappingFunction;
    private int mappingParamIndex;

    public String getClassQName()
    {
        return classQName;
    }

    public String getMappingFunction()
    {
        return mappingFunction;
    }

    public int getMappingParamIndex()
    {
        return mappingParamIndex;
    }

    RobotlegsEnum(String classQName, String mappingFunction, int mappingParamIndex)
    {
        //To change body of created methods use File | Settings | File Templates.
        this.classQName = classQName;
        this.mappingFunction = mappingFunction;
        this.mappingParamIndex = mappingParamIndex;
    }
}
