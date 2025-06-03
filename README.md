# CoorApp - Solución Móvil para Gestión Logística

![CoorApp Icon](https://raw.githubusercontent.com/Enrique213-VP/CoorApp/refs/heads/main/app/src/main/res/drawable/icon.png)

## 📱 Descripción del Proyecto

CoorApp es una aplicación Android innovadora desarrollada para optimizar la gestión logística mediante el uso de códigos QR y geolocalización. La solución permite a los operadores capturar, validar y trackear información de manera eficiente, proporcionando trazabilidad completa en tiempo real.

## 🎯 Propósito de la Aplicación

Esta aplicación fue diseñada para resolver desafíos específicos en el sector logístico, permitiendo:
- Captura rápida de información mediante códigos QR
- Validación automática de datos estructurados
- Tracking geoespacial de elementos en campo
- Sincronización robusta entre dispositivos móviles y sistemas centralizados

## 🏗️ Stack Tecnológico

### Core Technologies
- **Java** - Desarrollo nativo Android
- **RxJava** - Programación reactiva para operaciones concurrentes
- **Dagger 2** - Framework de inyección de dependencias
- **Firebase Firestore** - Base de datos NoSQL en tiempo real
- **SQLite** - Persistencia local optimizada
- **Volley** - Cliente HTTP ligero y eficiente

### Arquitectura de Software
- **MVVM Pattern** - Separación clara de capas de presentación y lógica
- **Repository Pattern** - Abstracción de fuentes de datos
- **Dependency Injection** - Desacoplamiento y testabilidad
- **Reactive Programming** - Manejo eficiente de streams de datos

## 🚀 Características Principales

### 🔐 Sistema de Autenticación
- Autenticación segura contra base de datos centralizada
- Validación de credenciales en tiempo real
- Gestión de sesiones con limpieza automática

### 📱 Captura Inteligente de Datos
- **Escaneo QR Avanzado**: Lectura optimizada con procesamiento Base64
- **Entrada Manual**: Alternativa para situaciones donde el escaneo no es viable
- **Validación en Tiempo Real**: Verificación inmediata de estructura de datos

### 🗺️ Geolocalización y Mapas
- **Visualización Interactiva**: Mapas integrados con marcadores personalizados
- **Routing Inteligente**: Cálculo de rutas desde ubicación actual
- **Vista Optimizada**: Interfaz horizontal para mejor experiencia de mapas

### 💾 Gestión Avanzada de Datos
- **Sincronización Inteligente**: Backup automático cada 5 registros exitosos
- **Persistencia Híbrida**: Combinación de almacenamiento local y en la nube
- **Recuperación Automática**: Restauración de datos al reconectar

### 🎨 Experiencia de Usuario
- **Design System Consistente**: Paleta de colores corporativa en tonos azules
- **Iconografía Moderna**: Sistema de iconos limpio y funcional
- **Responsive Design**: Adaptación automática a diferentes tamaños de pantalla

## 💡 Funcionalidades Implementadas

### Dashboard Principal
- ✅ Panel de control centralizado
- ✅ Acceso rápido a funciones principales
- ✅ Lista dinámica de elementos procesados
- ✅ Estado en tiempo real de sincronización

### Motor de Procesamiento QR
- ✅ Engine de escaneo optimizado para rendimiento
- ✅ Validación contra API RESTful externa
- ✅ Procesamiento de respuestas estructuradas JSON
- ✅ Cache local para operación offline

### Sistema de Backup Distribuido
- ✅ Sincronización automática basada en triggers
- ✅ Identificación única por dispositivo
- ✅ Versionado de datos con timestamps
- ✅ Limpieza inteligente de datos obsoletos

### Módulo de Geolocalización
- ✅ Integración nativa con servicios de mapas
- ✅ Renderizado de coordenadas desde datos QR
- ✅ Cálculo de distancias y rutas
- ✅ Optimización para uso en campo

## 🔧 DevOps y Automatización

### Pipeline de CI/CD
- ✅ **Continuous Integration**: Compilación automática en cada commit
- ✅ **Automated Testing**: Ejecución de suite de pruebas completa
- ✅ **Continuous Deployment**: Distribución automática via Firebase
- ✅ **Quality Gates**: Validación de código y cobertura de pruebas

### Infraestructura de Distribución
- ✅ **Firebase App Distribution**: Entrega controlada a stakeholders
- ✅ **Gestión de Releases**: Versionado automático y release notes
- ✅ **Testing Environment**: Ambiente dedicado para QA

## 🧪 Quality Assurance

### Testing Strategy
- ✅ **Unit Testing**: Cobertura de lógica de negocio crítica
- ✅ **Integration Testing**: Validación de flujos end-to-end
- ✅ **Data Layer Testing**: Verificación de operaciones de persistencia
- ✅ **API Testing**: Validación de contratos de servicios externos

## 🌐 Integración de Servicios

### API de Validación de Datos
```http
POST https://noderedtest.coordinadora.com/api/v1/validar
Content-Type: application/json

{
  "data": "[payload_encoded_base64]"
}
```

### Estructura de Payload QR
```
etiqueta1d:[ID]-latitud:[LAT]-longitud:[LON]-observacion:[NOTE]
```

## 🔐 Seguridad y Permisos

### Permisos del Sistema
- **CAMERA** - Acceso a cámara para captura QR
- **ACCESS_FINE_LOCATION** - Servicios de geolocalización
- **INTERNET** - Conectividad para sincronización

### Consideraciones de Seguridad
- Encriptación de datos sensibles en tránsito
- Validación de entrada robusta
- Gestión segura de credenciales en CI/CD

## 👥 Equipo de Stakeholders

### Colaboradores del Proyecto
- **Technical Lead**: [@ingoskr10](https://github.com/ingoskr10)

### Business Stakeholders
- **Product Owner**: oscart@coordinadora.com
- **Technical Reviewer**: sdhajan@coordinadora.com  
- **QA Lead**: camilov@coordinadora.com

## 📈 Métricas de Éxito

### Criterios de Aceptación Cumplidos
- 🎯 **Funcionalidad**: 100% de requisitos implementados
- 🏗️ **Arquitectura**: Patrón MVVM aplicado consistentemente
- ⚡ **Performance**: Operaciones asíncronas optimizadas con RxJava
- 🔧 **Maintainability**: Inyección de dependencias implementada
- 📝 **Code Quality**: Estándares de desarrollo siguiendo best practices
- 🛡️ **Reliability**: Manejo robusto de errores y edge cases
- 🧪 **Testing**: Cobertura comprehensiva de casos críticos
- 🚀 **Deployment**: Pipeline de CI/CD completamente automatizado
- 🎨 **UX/UI**: Implementación fiel del design system corporativo

## 🚀 Demo y Testing

La aplicación está disponible para evaluación a través de Firebase App Distribution para todos los stakeholders autorizados del proyecto.

---

**Desarrollado por:** Sergio Enrique Vargas Pedraza  
**Contacto:** colombia00028@gmail.com  
**Entrega:** Mayo 2025

*Solución empresarial desarrollada para optimización de procesos logísticos*
