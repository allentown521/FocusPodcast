package allen.town.podcast.service

import allen.town.core.service.AliPayService
import allen.town.podcast.BuildConfig
import android.content.Context
import com.wyjson.router.annotation.Service


@Service(remark = "/app/pay/alipay_service")
class AliPayServiceImpl : AliPayService {
    override fun getWeeklyPrice(): String {
        return "2.00"
    }

    override fun getMonthPrice(): String {
        return "9.69"
    }

    override fun getQuarterlyPrice(): String {
        return "22.69"
    }

    override fun getYearlyPrice(): String {
        return "45.69"
    }

    override fun getRemoveAdPrice(): String {
        return "22.99"
    }

    override fun getAppId(): String {
        return BuildConfig.ALIPAY_APP_ID
    }

    override fun getPid(): String {
        return BuildConfig.ALIPAY_PID
    }

    override fun getSandboxPrivateKey(): String {
        return "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCfMEiOAjaIhdjl3eTQ8xVoh9ejYU440AYTTV2sX1/ZlG1oL9ANCes4KXT91ih18cwfEOg37EGvWJeZkpKPUO1XzRd6WZDzdyA1j98MPZE+XT9vp0lQSH7o1CkWurw6QpwGMW7dQmDJA5gbQHavwlBrxLTeeLFZxYtZXPoQetEG4hZAcLhQCrG7htnd3FOX5x66nVScPYtNwkjvXlet4PpfwHG2CN66dciMGtWDDUMn5taMvjc6EFpdkAH5N2n6IN1deMkhqZfs9YDbdmdWcT5ocQmVxwHBL9njON5FLkc9ouMKw1kseH7G0ihNoRbQyOtmhwad0eLDbwi36ZJuAfUhAgMBAAECggEAUo+yMaTIL8prkdyhvhU09z/tARmIfkK1w/EOMkZM6gXnMHAL0ZdhXYFy4evelD1SBpK4PbjU4GJsTgQM6zOnxS/ji1tEqGESXXU+05Ri7htiuT/j8DWJTKwsm1NCKF0/mp6sxyiUFe09SHUImWOIXjxpKjEimlKwUSq9rypjmtN5fqNKdNfglAf6m2ML9e4U4VsvetG04fshv4NJxdqo+d7+5rm32vWn4PedH5q3YGieObr7yKoVLJVjoN8KxBs8hft4IESXIKD4XEI5Fy9EbCf+wDmQE4olx4hwFgZbxapHLcL09ynbbcUfdUcczc7X7mPC0fP0r5u2PGzw5MZYgQKBgQDZ+F+UgStBa6qE5JSaJsNlG6ifCZW8qxM1S6Lm69DqOejK1OFg5uc1+4AOXjvW1z3HVvmuTmjEDqZUjVQELS+4vTDJFlp2NEzljJKvhG5OjHPtJd0rn9qwtrcF02BQ8JglK2MVYwMsQVLKgkZGRbqJNpXdc9hVE12Abz0OkkYMSQKBgQC69m7qWxgYkbaIjhuUMr9K0eJe+WScu7gKmMX9eGpM/9aICDVn1DVU3G/U9nReuDSKVZ4NSLOMb9Jfe3GNZKi21f8y2sE9jI18QOgLzW323jrg74bmEupaOOM3nFb+eujArI01Q9spGa+HiBfQCgbqBLZ69ad/xcllRp3Nq6CyGQKBgE1FWJTryarSgUvFQMz5CvHQAVIH8tZEi3WsT56vYIt4ZgdpulBA6xxfAUGWtH3wJg2Bzte1IjzGuL5mr68fWbiTETVoQD+BQPVrhSDFwNkFv58FekeZzswwqeddzNpEwJsEq4aMaDaLHc9+qW0GER/NgwlTpRBb4hzC0pFU15JJAoGAGbcHbmh1GfwAtSuGk42fTfUsQ4/dzMjs+Vgl80T6qfiOU0t9vPgtFaz0HMCBJP4FC/nWsVEMkQZYUxWxepcbtXodjasU5RhN5Ycv41+v/kJ2qrWHQmHbpekpJ17zgDD7jnStP2RD4pVL1UJctBqM9OcNCkB7d1GG780Uq1Mt2RECgYBPOyqrOKPRtlnn9zysJlSksG1+1V2FL0B5fYvffYT/uGLvVhf26W5EXT87SyH+My10qQoYcUW/b0paVpFBz4W6PEcGTUr2rti4cawA9952SegUoaph9etCkdyyAMGBXhNLCIpeSCbA84GXWC6EZZAcVjnVdbkveIOenYGGguHuLQ=="
    }

    override fun getSandboxPublicKey(): String {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzBIjgI2iIXY5d3k0PMVaIfXo2FOONAGE01drF9f2ZRtaC/QDQnrOCl0/dYodfHMHxDoN+xBr1iXmZKSj1DtV80XelmQ83cgNY/fDD2RPl0/b6dJUEh+6NQpFrq8OkKcBjFu3UJgyQOYG0B2r8JQa8S03nixWcWLWVz6EHrRBuIWQHC4UAqxu4bZ3dxTl+ceup1UnD2LTcJI715XreD6X8BxtgjeunXIjBrVgw1DJ+bWjL43OhBaXZAB+Tdp+iDdXXjJIamX7PWA23ZnVnE+aHEJlccBwS/Z4zjeRS5HPaLjCsNZLHh+xtIoTaEW0MjrZocGndHiw28It+mSbgH1IQIDAQAB"
    }

    override fun getSandboxAlipayPublicKey(): String {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApD6XCuOyQlTVDaPcSGjJwvmIpZW9pY6AKnhprnvqxj8jmk/CfmaAOsVSru0mExAx28hjokZpa44ME0wMuzia0ceRSEdxgxNGhtatS5uwXkagkBUleit0fLCdHQIrHYmzJF1gIrfRuBRqte060Zt1IMyp5d2uE0NBnP2my1gue0esQh+PZ4jOIp2JKCLq5KQr5GBJacR9GvPh7fHZuRZImddyHPF4SutrknkQAQMOdMmsqwwxqXZtcwG9MS6ArxOSy/S5qfC/lWaHGsOXS4WFEFZuM5GJ5DQ++Jl7I0R9Her/6hZ4kYDHy5px/zeCC5/Bz3j+YFus0+2WnUCYP4iErwIDAQAB"
    }

    override fun getPrivateKey(): String {
        return BuildConfig.ALIPAY_APP_PRIVATE_KEY
    }

    override fun getPublicKey(): String {
        return BuildConfig.ALIPAY_APP_PUBLIC_KEY
    }

    override fun getAlipayPublicKey(): String {
        return BuildConfig.ALIPAY_PUBLIC_KEY
    }

    override fun init() {

    }

}