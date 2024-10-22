export interface CopyDbFromStorageRequest {
    dbName: string;            // Name of the database to copy
    base64Source: string;        // base64 Source of the source database
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
