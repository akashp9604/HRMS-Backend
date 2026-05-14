package com.configserver.hrm.payrollService.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payroll.default")
public class PayrollDefaults {
    private double basicPercent;
    private double hraPercentOfBasic;
    private double employeePfPercent;
    private double employerPfPercent;
    private double gratuityPercentOfBasic;
    private int workingDaysPerMonth;
    private double professionalTax;

    // getters & setters
    public double getBasicPercent() { return basicPercent; }
    public void setBasicPercent(double basicPercent) { this.basicPercent = basicPercent; }
    public double getHraPercentOfBasic() { return hraPercentOfBasic; }
    public void setHraPercentOfBasic(double hraPercentOfBasic) { this.hraPercentOfBasic = hraPercentOfBasic; }
    public double getEmployeePfPercent() { return employeePfPercent; }
    public void setEmployeePfPercent(double employeePfPercent) { this.employeePfPercent = employeePfPercent; }
    public double getEmployerPfPercent() { return employerPfPercent; }
    public void setEmployerPfPercent(double employerPfPercent) { this.employerPfPercent = employerPfPercent; }
    public double getGratuityPercentOfBasic() { return gratuityPercentOfBasic; }
    public void setGratuityPercentOfBasic(double gratuityPercentOfBasic) { this.gratuityPercentOfBasic = gratuityPercentOfBasic; }
    public int getWorkingDaysPerMonth() { return workingDaysPerMonth; }
    public void setWorkingDaysPerMonth(int workingDaysPerMonth) { this.workingDaysPerMonth = workingDaysPerMonth; }
    public double getProfessionalTax() { return professionalTax; }
    public void setProfessionalTax(double professionalTax) { this.professionalTax = professionalTax; }
}