package com.waaagh.entity;

import static com.waaagh.enums.SpringBootMethodAnnotation.REQUEST_MAPPING;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceExpression;
import com.waaagh.enums.SpringBootMethodAnnotation;
import com.waaagh.utils.AnnotationParserUtils;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class HttpMappingInfo implements Serializable {
    /**
     * full url path
     */
    private String path = "";
    /**
     * psiMethod
     */
    private PsiMethod psiMethod;
    /**
     * HTTP 请求方法集合（GET/POST/PUT/DELETE/PATCH/...）。
     * 空集合表示注解未指定方法：按 Spring 语义匹配所有请求方法（通配）。
     */
    private Set<String> requestMethods = Collections.emptySet();
    /**
     * swagger info
     */
    private String swaggerInfo = "";
    /**
     * swagger notes
     */
    private String swaggerNotes = "";

    public HttpMappingInfo() {
    }

    public HttpMappingInfo(String path, String swaggerInfo, String swaggerNotes, PsiMethod psiMethod) {
        this.path = path;
        this.swaggerInfo = swaggerInfo;
        this.swaggerNotes = swaggerNotes;
        this.psiMethod = psiMethod;
    }

    public String getPath() {
        return this.path;
    }

    public String getSwaggerInfo() {
        return this.swaggerInfo;
    }

    public String getSwaggerNotes() {
        return this.swaggerNotes;
    }

    public PsiMethod getPsiMethod() {
        return this.psiMethod;
    }

    public Set<String> getRequestMethods() {
        return this.requestMethods;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setSwaggerInfo(String swaggerInfo) {
        this.swaggerInfo = swaggerInfo;
    }

    public void setSwaggerNotes(String swaggerNotes) {
        this.swaggerNotes = swaggerNotes;
    }

    public void setPsiMethod(PsiMethod psiMethod) {
        this.psiMethod = psiMethod;
    }

    public void setRequestMethods(Set<String> requestMethods) {
        this.requestMethods = requestMethods;
    }

    /**
     * 判断两个映射的 HTTP 请求方法是否兼容：
     * 任一方未指定方法（空集合）视为通配；否则要求集合存在交集。
     */
    public boolean isRequestMethodCompatibleWith(HttpMappingInfo other) {
        if (other == null) {
            return false;
        }
        if (this.requestMethods.isEmpty() || other.requestMethods.isEmpty()) {
            return true;
        }
        return !Collections.disjoint(this.requestMethods, other.requestMethods);
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof HttpMappingInfo)) return false;
        final HttpMappingInfo other = (HttpMappingInfo) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$path = this.getPath();
        final Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        final Object this$swaggerInfo = this.getSwaggerInfo();
        final Object other$swaggerInfo = other.getSwaggerInfo();
        if (this$swaggerInfo == null ? other$swaggerInfo != null : !this$swaggerInfo.equals(other$swaggerInfo))
            return false;
        final Object this$swaggerNotes = this.getSwaggerNotes();
        final Object other$swaggerNotes = other.getSwaggerNotes();
        if (this$swaggerNotes == null ? other$swaggerNotes != null : !this$swaggerNotes.equals(other$swaggerNotes))
            return false;
        final Object this$method = this.getPsiMethod();
        final Object other$method = other.getPsiMethod();
        if (this$method == null ? other$method != null : !this$method.equals(other$method)) return false;
        final Object this$requestMethods = this.getRequestMethods();
        final Object other$requestMethods = other.getRequestMethods();
      return this$requestMethods == null ? other$requestMethods == null
          : this$requestMethods.equals(other$requestMethods);
    }

    protected boolean canEqual(final Object other) {
        return other instanceof HttpMappingInfo;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        final Object $swaggerInfo = this.getSwaggerInfo();
        result = result * PRIME + ($swaggerInfo == null ? 43 : $swaggerInfo.hashCode());
        final Object $swaggerNotes = this.getSwaggerNotes();
        result = result * PRIME + ($swaggerNotes == null ? 43 : $swaggerNotes.hashCode());
        final Object $method = this.getPsiMethod();
        result = result * PRIME + ($method == null ? 43 : $method.hashCode());
        final Object $requestMethods = this.getRequestMethods();
        result = result * PRIME + ($requestMethods == null ? 43 : $requestMethods.hashCode());
        return result;
    }

    public String toString() {
        return "ControllerInfo(path=" + this.getPath() + ", swaggerInfo=" + this.getSwaggerInfo() + ", swaggerNotes=" + this.getSwaggerNotes() + ", method=" + this.getPsiMethod() + ", requestMethods=" + this.getRequestMethods() + ")";
    }
    /**
     * 根据方法提取完整的接口信息
     *
     * @param parentPath 父路径
     * @param method     方法
     * @return {@link HttpMappingInfo}
     */
    public static HttpMappingInfo of(String parentPath, PsiMethod method) {
        HttpMappingInfo httpMappingInfo = new HttpMappingInfo();
        httpMappingInfo.setPath(parentPath);
        PsiAnnotation[] annotations = method.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String annotationName = annotation.getQualifiedName();
            // 处理 @RequestMapping 注解
            if (annotationName != null && annotationName.equals(REQUEST_MAPPING.getQualifiedName())) {
                // 未指定 method 属性时集合为空：Spring 语义为匹配所有请求方法
                httpMappingInfo.setRequestMethods(
                    extractRequestMethods(annotation.findAttributeValue("method")));
                return AnnotationParserUtils.getValue(annotation, httpMappingInfo, method);
            } else if (SpringBootMethodAnnotation.getByQualifiedName(annotationName) != null) {
                // 处理其他常用注解（@GetMapping/@PostMapping/... 方法唯一）
                SpringBootMethodAnnotation requestMethod = SpringBootMethodAnnotation.getByQualifiedName(annotationName);
                if (requestMethod != null && requestMethod.methodName() != null) {
                    httpMappingInfo.setRequestMethods(Collections.singleton(requestMethod.methodName()));
                }
                return AnnotationParserUtils.getValue(annotation, httpMappingInfo, method);
            }

        }
        return null;
    }

    /**
     * 提取 @RequestMapping 的 method 属性，支持单值、数组及常量/枚举引用：
     * {@code method = RequestMethod.GET}、{@code method = {GET, POST}}。
     * 返回空集合表示“未指定方法”（通配）。
     */
    private static Set<String> extractRequestMethods(PsiAnnotationMemberValue methodValue) {
        if (methodValue == null) {
            return Collections.emptySet();
        }
        List<PsiAnnotationMemberValue> values;
        if (methodValue instanceof PsiArrayInitializerMemberValue) {
            values = Arrays.asList(((PsiArrayInitializerMemberValue) methodValue).getInitializers());
        } else {
            values = Collections.singletonList(methodValue);
        }
        Set<String> requestMethods = new LinkedHashSet<>();
        for (PsiAnnotationMemberValue value : values) {
            String requestMethod = resolveRequestMethod(value);
            if (requestMethod != null) {
                requestMethods.add(requestMethod);
            }
        }
        return requestMethods;
    }

    /**
     * 将 method 属性中的一个引用（如 {@code RequestMethod.GET}）解析为标准 HTTP 方法名，解析失败返回 null。
     */
    private static String resolveRequestMethod(PsiAnnotationMemberValue methodValue) {
        if (!(methodValue instanceof PsiReferenceExpression)) {
            return null;
        }
        PsiElement resolvedElement = ((PsiReferenceExpression) methodValue).resolve();
        if (!(resolvedElement instanceof PsiField)) {
            return null;
        }
        return AnnotationParserUtils.getRequestMethodFromMethodName(((PsiField) resolvedElement).getName());
    }
}