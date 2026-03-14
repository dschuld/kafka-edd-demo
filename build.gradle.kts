plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.spring") version "2.2.21"
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "net.davidschuld"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	maven("https://packages.confluent.io/maven/")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-kafka")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.apache.avro:avro:1.12.1")
	implementation("io.confluent:kafka-avro-serializer:7.7.1")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
	}
	sourceSets["main"].kotlin.srcDir(tasks.named("generateAvroJava").map { it.outputs })
}

tasks.withType<Test> {
	useJUnitPlatform()
}
