package com.infowings.catalog.objects

import com.infowings.catalog.common.DetailedObjectResponse
import com.infowings.catalog.common.ObjectEditDetailsResponse
import com.infowings.catalog.common.ObjectsResponse
import com.infowings.catalog.common.objekt.*
import com.infowings.catalog.utils.delete
import com.infowings.catalog.utils.encodeURIComponent
import com.infowings.catalog.utils.get
import com.infowings.catalog.utils.post
import kotlinx.serialization.json.JSON

suspend fun getAllObjects(): ObjectsResponse = JSON.parse(get("/api/objects"))

suspend fun getDetailedObject(id: String): DetailedObjectResponse = JSON.parse(get("/api/objects/${encodeURIComponent(id)}/viewdetails"))

suspend fun getDetailedObjectForEdit(id: String): ObjectEditDetailsResponse = JSON.parse(get("/api/objects/${encodeURIComponent(id)}/editdetails"))

suspend fun createObject(request: ObjectCreateRequest): ObjectCreateResponse =
    JSON.parse(post("/api/objects/create", JSON.stringify(request)))

suspend fun createProperty(request: PropertyCreateRequest): PropertyCreateResponse =
    JSON.parse(post("/api/objects/createProperty", JSON.stringify(request)))

suspend fun createValue(request: ValueCreateRequest): ValueCreateResponse =
    JSON.parse(post("/api/objects/createValue", JSON.stringify(request.toDTO())))

suspend fun updateObject(request: ObjectUpdateRequest): ObjectUpdateResponse =
    JSON.parse(post("/api/objects/update", JSON.stringify(request)))

suspend fun updateProperty(request: PropertyUpdateRequest): PropertyUpdateResponse =
    JSON.parse(post("/api/objects/updateProperty", JSON.stringify(request)))

suspend fun updateValue(request: ValueUpdateRequest): ValueUpdateResponse =
    JSON.parse(post("/api/objects/updateValue", JSON.stringify(request.toDTO())))

suspend fun deleteObject(id: String, force: Boolean) {
    delete("/api/objects/object/${encodeURIComponent(id)}?force=$force")
}

suspend fun deleteProperty(id: String, force: Boolean) {
    delete("/api/objects/property/${encodeURIComponent(id)}?force=$force")
}

suspend fun deleteValue(id: String, force: Boolean): ValueDeleteResponse =
    JSON.parse(delete("/api/objects/value/${encodeURIComponent(id)}?force=$force"))