package br.com.ecociente.mobile.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.ecociente.mobile", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

  @ArchTest
  static final ArchRule core_independente_das_camadas_externas = noClasses()
      .that().resideInAPackage(
          "br.com.ecociente.mobile.core..")
      .should().dependOnClassesThat()
      .resideInAnyPackage(
          "br.com.ecociente.mobile.dataprovider..",
          "br.com.ecociente.mobile.entrypoint..",
          "br.com.ecociente.mobile.config..");

  @ArchTest
  static final ArchRule core_sem_tecnologias_dos_adapters = noClasses()
      .that().resideInAPackage(
          "br.com.ecociente.mobile.core..")
      .should().dependOnClassesThat()
      .resideInAnyPackage(
          "org.springframework.data..",
          "org.springframework.jdbc..",
          "org.springframework.dao..",
          "org.springframework.security..",
          "org.springframework.web..",
          "org.springframework.http..",
          "jakarta.persistence..",
          "org.hibernate..",
          "org.postgresql..",
          "com.google.firebase..",
          "ai.onnxruntime..");

  @ArchTest
  static final ArchRule entrypoint_sem_acesso_ao_dataprovider = noClasses()
      .that().resideInAPackage(
          "br.com.ecociente.mobile.entrypoint..")
      .should().dependOnClassesThat()
      .resideInAPackage(
          "br.com.ecociente.mobile.dataprovider..");

  @ArchTest
  static final ArchRule dataprovider_independente_do_entrypoint = noClasses()
      .that().resideInAPackage(
          "br.com.ecociente.mobile.dataprovider..")
      .should().dependOnClassesThat()
      .resideInAPackage(
          "br.com.ecociente.mobile.entrypoint..");

  @ArchTest
  static final ArchRule dominio_sem_frameworks = noClasses()
      .that().resideInAPackage(
          "br.com.ecociente.mobile.core.domain..")
      .should().dependOnClassesThat()
      .resideInAnyPackage(
          "org.springframework..",
          "jakarta.persistence..",
          "com.fasterxml.jackson..");

  @ArchTest
  static final ArchRule gateways_independentes_dos_services = noClasses()
      .that().resideInAPackage(
          "br.com.ecociente.mobile.core.gateway..")
      .should().dependOnClassesThat()
      .resideInAPackage(
          "br.com.ecociente.mobile.core.service..");

  @ArchTest
  static final ArchRule dominio_independente_da_orquestracao = noClasses()
      .that().resideInAPackage(
          "br.com.ecociente.mobile.core.domain..")
      .should().dependOnClassesThat()
      .resideInAnyPackage(
          "br.com.ecociente.mobile.core.service..",
          "br.com.ecociente.mobile.core.gateway..");

  @ArchTest
  static final ArchRule sem_pacote_usecase = noClasses()
      .should().resideInAPackage("..usecase..");
}