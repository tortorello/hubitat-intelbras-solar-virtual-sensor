import groovy.transform.Field
import groovy.json.JsonSlurper

metadata {
    definition(name: "Intelbras Solar Virtual Power/Energy Sensor", namespace: "tortorello.intelbras", author: "Victor Tortorello Neto") {
        capability "PowerMeter"
        capability "EnergyMeter"
        capability "Sensor"
        capability "Refresh"
        
        attribute "energyThisMonth", "number"
        attribute "nominalPower", "number"
        attribute "inverters", "number"
        attribute "lastUpdate", "string"
    }

    preferences {
        input name: "pollInterval", type: "number", title: "Poll Interval (Minutes)", defaultValue: 5
        input name: "debugLogging", type: "bool", title: "Enable Debug Logging?", defaultValue: true
	    input name: "apiUsername", type: "string", title: "API Username", required: true
    	input name: "apiPassword", type: "string", title: "API Password", required: true, description: "⚠️ Caution: the password is stored open-text."
        input name: "plantId", type: "string", title: "Plant ID", description: "The first (or unique) plant if not specified."
    }
}

@Field static final _apiBaseUrl = "http://solar-monitoramento.intelbras.com.br"
@Field static final _apiRequestTimeout = 5

def installed() {
    log.info "Instalado"
    initialize()
}

def updated() {
    log.info "Atualizado"
    unschedule()
    initialize()
}

def initialize() {
    if (pollInterval) {
        schedule("0 */${pollInterval} * ? * *", refresh)
    } else {
        schedule("0 */5 * ? * *", refresh)
    }
    
    refresh()
}

def postFormEncodedRequest(endpoint, params, sessionCookie = null) {
    def httpParams = [
        uri: "$_apiBaseUrl$endpoint",
        headers: [
            "Content-Type": "application/x-www-form-urlencoded"
        ],
        body: ""
    ]
    
    if (sessionCookie) httpParams.headers += ["Cookie": sessionCookie?.collect { it -> it.value }.join("; ")]
    
    httpParams.body = params.collect { k, v -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v.toString(), "UTF-8")}" }.join("&")
    
    def responseData = [status: null, error: false, data: null, sessionCookie: null]
    
    try {
        log.debug "Sending HTTP post request: $httpParams"
        
        httpPost(httpParams) { res ->
            responseData.status = res.status
            
            if (res.status == 200) {
                log.debug "Resposta: ${res.data}"
                responseData.data = new groovy.json.JsonSlurper().parseText(res.data.toString())
                
                if (endpoint.equals("/login")) {
                    def setCookie = res.headers?.findAll { it.name == "Set-Cookie" }
                    
                    if (setCookie) {
                        responseData.sessionCookie = setCookie
                        log.info "Cookie de sessão salvo: ${responseData.sessionCookie}"
                    } else {
                        log.warn "Nenhum cookie encontrado na resposta."
                    }
                }
            } else {
                responseData.error = true
                log.warn "Erro na chamada: ${res.status}"
            }
        }
    } catch (Exception e) {
        responseData.error = true
        log.error "Erro durante chamada POST: ${e.message}"
    }
    
    return responseData
}

def refresh() {
    def loginResponse = postFormEncodedRequest("/login", [
        account: settings.apiUsername,
        password: settings.apiPassword,
        validateCode: "",
        lang: "en"
    ])
    
    if (loginResponse.error) {
        log.debug "Unable to login."
        return
    }
    
    def invTotalDataResponse = postFormEncodedRequest("/panel/intelbras/getInvTotalData", [
        plantId: settings.plantId
    ], loginResponse.sessionCookie)
    
    if (invTotalDataResponse.error) {
        log.debug "Unable to get inversor total data."
        return
    }
    
	def energyThisDay = invTotalDataResponse.data.obj?.eToday ?: 0f
    def energyThisMonth = 0f
   
    def devicesByPlantListResponse = postFormEncodedRequest("/panel/getDevicesByPlantList", [
        plantId: settings.plantId,
        currPage: 1
    ], loginResponse.sessionCookie)
    
    if (devicesByPlantListResponse.error) {
        log.debug "Unable to get devices by plant."
        return
    }  
    
    def currentPower = 0f
    def nominalPower = 0f
    
	def inverters = 0    
    
    for (e in devicesByPlantListResponse.data?.obj?.datas) {
        inverters++
        currentPower += e.pac as float
        nominalPower += e.nominalPower as float
        energyThisMonth += e.eMonth as float
    }
    
    sendEvent(name: "energy", value: energyThisDay, unit: "kWh")
    sendEvent(name: "energyThisMonth", value: energyThisMonth, unit: "kWh")
    sendEvent(name: "power", value: currentPower, unit: "W")
    sendEvent(name: "nominalPower", value: nominalPower, unit: "W")
    sendEvent(name: "inverters", value: inverters)
    sendEvent(name: "lastUpdate", value: new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone))
    
    if (debugLogging) log.debug "Refresh: Power=${currentPower}W, Energy=${energyThisDay}kWh"
}


