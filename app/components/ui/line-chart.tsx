import React, { useState } from 'react';
import { View, Dimensions, StyleSheet, LayoutChangeEvent } from 'react-native';
import Svg, { Path, Line, Text as SvgText, Circle, Defs, LinearGradient, Stop } from 'react-native-svg';
import { Colors } from '@/constants/theme';

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
    const [width, setWidth] = useState(Dimensions.get('window').width - 64); // Fallback width

    const onLayout = (event: LayoutChangeEvent) => {
        setWidth(event.nativeEvent.layout.width);
    };

    // Calculate dimensions
    const paddingRight = 20;
    const paddingLeft = 10;
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

    return (
        <View style={styles.container} onLayout={onLayout}>
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
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        width: '100%',
        alignItems: 'center',
        justifyContent: 'center',
    },
});
