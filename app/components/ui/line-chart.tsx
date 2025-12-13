import React, { useState } from 'react';
import { View, Text, Dimensions, StyleSheet, LayoutChangeEvent, Pressable, GestureResponderEvent, Platform } from 'react-native';
import { useTranslation } from 'react-i18next';
import Svg, { Path, Line, Text as SvgText, Circle, Defs, LinearGradient, Stop } from 'react-native-svg';
import { Colors } from '@/constants/theme';

const TOOLTIP_BG_COLOR = 'rgba(255, 255, 255, 0.95)';

interface DualLineChartProps {
    data1: number[];
    data2: number[];
    labels: string[];
    color1: string;
    color2: string;
    height?: number;
    isPersonalMode?: boolean;
}

export function DualLineChart({
    data1,
    data2,
    labels,
    color1,
    color2,
    height = 200,
    isPersonalMode = false
}: DualLineChartProps) {
    const { t } = useTranslation();
    const [width, setWidth] = useState(Dimensions.get('window').width - 64); // Fallback width
    const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

    const onLayout = (event: LayoutChangeEvent) => {
        setWidth(event.nativeEvent.layout.width);
    };

    // Calculate dimensions
    const paddingRight = 20;
    const paddingLeft = 15;
    const paddingBottom = 24;
    const paddingTop = 20;
    const chartHeight = height;
    const chartWidth = width;
    const drawingHeight = chartHeight - paddingBottom - paddingTop;
    const drawingWidth = chartWidth - paddingRight - paddingLeft;

    // Calculate min/max for scaling
    const allData = [...data1, ...data2];
    const maxValue = Math.max(...allData, 1); // Ensure positive max for scaling when all 0
    const minValue = Math.min(...allData, 0); // Allow negative values
    const range = maxValue - minValue || 1;

    // Helper to map data point to coordinate
    const getCoordinate = (value: number, index: number) => {
        const x = paddingLeft + (index / (labels.length - 1)) * drawingWidth;
        // Normalize value within range [minValue, maxValue]
        const normalizedValue = (value - minValue) / range;
        const y = paddingTop + drawingHeight - normalizedValue * drawingHeight;
        return { x, y };
    };

    // Generate path strings
    const createPath = (data: number[]) => {
        if (data.length === 0) return '';

        // Move to first point
        // Ensure data exists and map properly
        const validData = data.map((d, i) => getCoordinate(d, i));
        if (validData.length === 0) return '';

        let path = `M ${validData[0].x} ${validData[0].y}`;

        // Draw lines to subsequent points
        for (let i = 1; i < validData.length; i++) {
            path += ` L ${validData[i].x} ${validData[i].y}`;
        }
        return path;
    };

    const path1 = createPath(data1);
    const path2 = createPath(data2);

    // Create area gradients
    const createArea = (data: number[]) => {
        if (data.length === 0) return '';
        const linePath = createPath(data);
        const lastX = getCoordinate(data[data.length - 1], data.length - 1).x;
        const firstX = getCoordinate(data[0], 0).x;
        const bottomY = paddingTop + drawingHeight;

        return `${linePath} L ${lastX} ${bottomY} L ${firstX} ${bottomY} Z`;
    };

    const area1 = createArea(data1);
    const area2 = createArea(data2);

    const handlePress = (event: GestureResponderEvent) => {
        let x = event.nativeEvent.locationX;

        // Web fallback if locationX is undefined
        if (Platform.OS === 'web' && x === undefined) {
            x = (event.nativeEvent as any).offsetX;
        }

        if (typeof x === 'number') {
            const index = Math.round(((x - paddingLeft) / drawingWidth) * (labels.length - 1));
            if (index >= 0 && index < labels.length) {
                setSelectedIndex(index);
            }
        }
    };

    return (
        <View style={styles.container} onLayout={onLayout}>
            <View style={{ width: chartWidth, height: chartHeight }}>
                <Svg width={chartWidth} height={chartHeight}>
                    <Defs>
                        <LinearGradient id="gradient1" x1="0" y1="0" x2="0" y2="1">
                            <Stop offset="0" stopColor={color1} stopOpacity="0.2" />
                            <Stop offset="1" stopColor={color1} stopOpacity="0" />
                        </LinearGradient>
                        <LinearGradient id="gradient2" x1="0" y1="0" x2="0" y2="1">
                            <Stop offset="0" stopColor={color2} stopOpacity="0.2" />
                            <Stop offset="1" stopColor={color2} stopOpacity="0" />
                        </LinearGradient>
                    </Defs>

                    {/* Grid Lines (Horizontal) */}
                    {[0, 0.5, 1].map((t) => {
                        const y = paddingTop + drawingHeight - t * drawingHeight;
                        return (
                            <Line
                                key={t}
                                x1={paddingLeft}
                                y1={y}
                                x2={chartWidth - paddingRight}
                                y2={y}
                                stroke={Colors.light.border}
                                strokeDasharray="4 4"
                                strokeWidth="1"
                            />
                        );
                    })}

                    {/* Zero Line */}
                    {minValue < 0 && maxValue > 0 && (
                        <Line
                            x1={paddingLeft}
                            y1={getCoordinate(0, 0).y}
                            x2={chartWidth - paddingRight}
                            y2={getCoordinate(0, 0).y}
                            stroke={Colors.light.textSecondary}
                            strokeWidth="1"
                            strokeOpacity="0.3"
                        />
                    )}

                    {/* Areas */}
                    <Path d={area1} fill="url(#gradient1)" />
                    <Path d={area2} fill="url(#gradient2)" />

                    {/* Lines */}
                    <Path d={path1} stroke={color1} strokeWidth="3" fill="none" />
                    <Path d={path2} stroke={color2} strokeWidth="3" fill="none" />

                    {/* Data Points */}
                    {data1.map((value, index) => {
                        const { x, y } = getCoordinate(value, index);
                        return (
                            <Circle
                                key={`d1-${index}`}
                                cx={x}
                                cy={y}
                                r="4"
                                fill={Colors.light.white}
                                stroke={color1}
                                strokeWidth="2"
                            />
                        );
                    })}
                    {data2.map((value, index) => {
                        const { x, y } = getCoordinate(value, index);
                        return (
                            <Circle
                                key={`d2-${index}`}
                                cx={x}
                                cy={y}
                                r="4"
                                fill={Colors.light.white}
                                stroke={color2}
                                strokeWidth="2"
                            />
                        );
                    })}


                    {/* Labels */}
                    {labels.map((label, index) => {
                        const x = paddingLeft + (index / (labels.length - 1)) * drawingWidth;
                        return (
                            <SvgText
                                key={index}
                                x={x}
                                y={chartHeight - 4}
                                fontSize="10"
                                fill={Colors.light.textSecondary}
                                textAnchor="middle"
                            >
                                {label}
                            </SvgText>
                        );
                    })}
                </Svg>

                <Pressable onPress={handlePress} style={StyleSheet.absoluteFill} />

                {selectedIndex !== null && (
                    <View style={styles.selectedInfoContainer}>
                        {/* <Text style={styles.selectedLabel}>{labels[selectedIndex]}</Text> */}
                        <View style={styles.summaryRow}>
                            <View style={styles.summaryItem}>
                                <View style={[styles.summaryDot, { backgroundColor: color1 }]} />
                                <Text style={styles.summaryLabel}>{isPersonalMode ? t('reports.income') : t('reports.sales')}:</Text>
                                <Text style={[styles.summaryValue, { color: color1 }]}>
                                    ₹{data1[selectedIndex]?.toLocaleString('en-IN') || 0}
                                </Text>
                            </View>
                            <View style={styles.summaryItem}>
                                <View style={[styles.summaryDot, { backgroundColor: color2 }]} />
                                <Text style={styles.summaryLabel}>{isPersonalMode ? t('personal.savings') : t('reports.profit')}:</Text>

                                <Text style={[styles.summaryValue, { color: data2[selectedIndex] >= 0 ? color2 : Colors.light.error }]}>
                                    ₹{data2[selectedIndex]?.toLocaleString('en-IN') || 0}
                                </Text>
                            </View>
                        </View>
                    </View>
                )}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        width: '100%',
        alignItems: 'center',
        justifyContent: 'center',
    },
    selectedInfoContainer: {
        position: 'absolute',
        top: -21,
        backgroundColor: TOOLTIP_BG_COLOR,
        paddingHorizontal: 12,
        paddingVertical: 8,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: Colors.light.border,
        alignSelf: 'center',
        shadowColor: Colors.light.shadow,
        shadowOffset: { width: 0, height: 2 },
        shadowOpacity: 0.1,
        shadowRadius: 4,
        elevation: 3,
        zIndex: 10,
    },
    summaryRow: {
        flexDirection: 'row',
        gap: 16,
    },
    summaryItem: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 6,
    },
    summaryDot: {
        width: 8,
        height: 8,
        borderRadius: 4,
    },
    summaryLabel: {
        fontSize: 12,
        color: Colors.light.textSecondary,
        fontWeight: '500',
    },
    summaryValue: {
        fontSize: 12,
        fontWeight: '700',
    },
});
