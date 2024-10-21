export interface ISensor {
    /* name string of the sensor. The name is guaranteed to be unique for a particular sensor type. */
    name: string;
    /* Maximum number of events of this sensor that could be batched. If this value is zero it indicates that batch mode is not supported for this sensor. If other applications registered to batched sensors, the actual number of events that can be batched might be smaller because the hardware FiFo will be partially used to batch the other sensors. */
    fifoMaxEventCount: number;
    /* Number of events reserved for this sensor in the batch mode FIFO. This gives a guarantee on the minimum number of events that can be batched. */
    fifoReservedEventCount: number;
    /* Get the highest supported direct report mode rate level of the sensor. */
    highestDirectReportRateLevel: number;
    /* The sensor id that will be unique for the same app unless the device is factory reset. Return value of 0 means this sensor does not support this function; return value of -1 means this sensor can be uniquely identified in system by combination of its type and name. */
    id: number;
    /* 	The max delay for this sensor in microseconds. */
    maxDelay: number;
    /* maximum range of the sensor in the sensor's unit. */
    maximumRange: number;
    /* 	the minimum delay allowed between two events in microseconds or zero if this sensor only returns a value when the data it's measuring changes. Note that if the app does not have the Manifest.permission.HIGH_SAMPLING_RATE_SENSORS permission, the minimum delay is capped at 5000 microseconds (200 Hz). */
    minDelay: number;
    /* the power in mA used by this sensor while in use */
    power: number;
    /* Reporting mode for the input sensor, one of REPORTING_MODE_* constants. */
    reportingMode: number;
    /* resolution of the sensor in the sensor's unit.*/
    resolution: number;
    /* The type of this sensor as a string.*/
    stringType: string;
    /* generic type of this sensor. */
    type: number;
    /* vendor string of this sensor.*/
    vendor: string;
    /* version of the sensor's module.*/
    version: number;
    /* Returns true if the sensor supports sensor additional information API */
    isAdditionalInfoSupported: boolean;
    /* true if the sensor is a dynamic sensor (sensor added at runtime).*/
    isDynamicSensor: boolean;
    /* Returns true if the sensor is a wake-up sensor. */
    isWakeUpSensor: boolean;
}

export interface CopyDbFromStorageRequest {
    dbName: string;            // Name of the database to copy
    sourcePath: string;        // Path of the source database
    location?: string;         // Location where the DB is located, optional (e.g., default: 'default')
    deleteOldDb?: boolean;     // Whether to delete the old DB (default: false)
    additionalOptions?: any;   // Any additional options required for customization
}

export interface CopyDbFromStorageResponse {
    success: boolean;          // Whether the operation was successful
    message?: string;          // Optional message, especially useful if an error occurred
}

export interface CopyDbToStorageRequest {
    fileName: string;          // File name of the database to be copied to storage
    fullPath: string;          // Full path where the database file will be copied
    overwrite?: boolean;       // Whether to overwrite the existing file, optional (default: false)
}

export interface CopyDbToStorageResponse {
    success: boolean;          // Whether the operation was successful
    message?: string;          // Optional message, especially useful if an error occurred
}

export default class DbCopyManager {
    copyDbFromStorage(request: CopyDbFromStorageRequest): Promise<CopyDbFromStorageResponse>;

    copyDbToStorage(
        request: CopyDbToStorageRequest
    ): Promise<CopyDbToStorageResponse>;
}
